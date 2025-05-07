package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlToBytesTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.fo.FOTreeBuilder;
import org.apache.xmlgraphics.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FoToPdfTransformer implements XmlToBytesTransformer {
  @SuppressWarnings("unused")
  static final Logger LOGGER = LoggerFactory.getLogger(FoToPdfTransformer.class);
  
  public static final URL CONFIG_URL =
      Resources.getResource(FoToPdfTransformer.class, "fop-config.xml");
  public static final URI CONFIG_URI = Unchecker.URI_UNCHECKER.getUsing(() -> CONFIG_URL.toURI());

  private final XmlTransformerFactory delegateTransformer;
  private final URI baseUri;
  private FopConfParser fopConfParser;
  private FopFactory fopFactory;

  private static FopConfParser parser(URI baseUri, ByteSource config)
      throws SAXException, IOException {
    try (InputStream configStream = config.openBufferedStream()) {
      return new FopConfParser(configStream, baseUri);
    }
  }

  public static FoToPdfTransformer usingFactory(TransformerFactory factory) {
    final XmlTransformerFactory transformer =
        XmlTransformerFactory.usingFactory(factory).pedantic();
    return new FoToPdfTransformer(transformer, CONFIG_URI, null);
  }

  /**
   * @param config the FOP conf
   * @return a fop factory
   * @throws SAXException iff a SAX error was thrown parsing the FOP conf
   * @throws IOException iff an I/O error is thrown while parsing the FOP conf
   */
  public static FoToPdfTransformer withConfig(TransformerFactory factory, ByteSource config)
      throws SAXException, IOException {
    final XmlTransformerFactory transformer =
        XmlTransformerFactory.usingFactory(factory).pedantic();
    return new FoToPdfTransformer(transformer, CONFIG_URI, parser(CONFIG_URI, config));
  }

  private FoToPdfTransformer(XmlTransformerFactory delegateTransformer, URI baseUri,
      FopConfParser fopConfParser) {
    this.delegateTransformer = checkNotNull(delegateTransformer);
    this.baseUri = checkNotNull(baseUri);
    checkArgument(baseUri.isAbsolute());
    this.fopConfParser = fopConfParser;
    if (fopConfParser != null) {
      this.fopFactory = fopConfParser.getFopFactoryBuilder().build();
      checkArgument(fopFactory.validateUserConfigStrictly());
      checkArgument(fopFactory.validateStrictly());
    } else {
      this.fopFactory = null;
    }
  }

  private FopConfParser lazyFopConfParser() {
    if (fopConfParser == null) {
      try {
        fopConfParser = parser(baseUri, Resources.asByteSource(CONFIG_URL));
      } catch (SAXException | IOException e) {
        throw new VerifyException(e);
      }
    }
    return fopConfParser;
  }

  private FopFactory lazyFactory() {
    if (fopFactory == null) {
      fopFactory = lazyFopConfParser().getFopFactoryBuilder().build();
      verify(fopFactory.validateUserConfigStrictly());
      verify(fopFactory.validateStrictly());
    }
    return fopFactory;
  }

  public FoToPdfTransformer withConfig(ByteSource config) throws SAXException, IOException {
    return new FoToPdfTransformer(delegateTransformer, baseUri, parser(baseUri, config));
  }

  /**
   * @param baseUri the base URI to resolve resource URIs against
   * @param config the FOP conf
   * @return a fop factory
   * @throws SAXException iff a SAX error was thrown parsing the FOP conf
   * @throws IOException iff an I/O error is thrown while parsing the FOP conf
   */
  public FoToPdfTransformer withConfig(@SuppressWarnings("hiding") URI baseUri, ByteSource config)
      throws SAXException, IOException {
    checkArgument(baseUri.isAbsolute());
    return new FoToPdfTransformer(delegateTransformer, baseUri, parser(baseUri, config));
  }

  public FoToPdfTransformer withConfig(@SuppressWarnings("hiding") FopConfParser fopConfParser) {
    return new FoToPdfTransformer(delegateTransformer, baseUri, fopConfParser);
  }

  @Override
  public void sourceToResult(Source source, Result result) throws XmlException {
    checkArgument(result instanceof StreamResult);
    final StreamResult streamResult = (StreamResult) result;

    final FOUserAgent foUserAgent = lazyFactory().newFOUserAgent();

    final FoEventListener l = new FoEventListener();
    foUserAgent.getEventBroadcaster().addEventListener(l);

    final Result res;
    try (OutputStream out = streamResult.getOutputStream()) {
      res = new SAXResult(new FOTreeBuilder(MimeConstants.MIME_PDF, foUserAgent, out));
    } catch (FOPException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new XmlException(e);
    }

    delegateTransformer.usingEmptyStylesheet().sourceToResult(source, res);
    /*
     * This duplicates the serious event that will get thrown in the log, but we’d better do that so
     * that one can see the order of events in the log and thus where the first serious one happened
     * exactly.
     */
    LOGGER.debug("Got {} serious and {} not serious events.", l.seriouses().size(),
        l.notSeriouses().size());
    l.logAll();
    l.seriouses().stream().findFirst().ifPresent(e -> {
      throw FoEventListener.asException(e);
    });
  }
}
