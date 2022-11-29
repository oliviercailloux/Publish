package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.model.EventSeverity;
import org.apache.xmlgraphics.util.MimeConstants;
import org.xml.sax.SAXException;

public class FoToPdfTransformer {

  private static class FoToBytesTransformer implements ToBytesTransformer {
    private final FopFactory fopFactory;
    private final XmlTransformer delegateTransformer;

    private FoToBytesTransformer(FopFactory fopFactory, XmlTransformer delegateTransformer) {
      this.fopFactory = fopFactory;
      this.delegateTransformer = delegateTransformer;
    }

    @Override
    public void toStream(Source source, OutputStream destination) throws XmlException {
      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      foUserAgent.getEventBroadcaster().addEventListener((e) -> {
        /* https://xmlgraphics.apache.org/fop/2.4/events.html */
        if (!e.getSeverity().equals(EventSeverity.INFO)) {
          throw new XmlException(e.toString());
        }
      });

      final Result res;
      try {
        final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, destination);
        res = new SAXResult(fop.getDefaultHandler());
      } catch (FOPException e) {
        throw new IllegalStateException(e);
      }

      delegateTransformer.usingEmptySource().transform(source, res);
    }
  }

  public static FoToPdfTransformer usingFactory(TransformerFactory factory) {
    return new FoToPdfTransformer(factory);
  }

  private final XmlTransformer transformer;

  private FoToPdfTransformer(TransformerFactory transformerFactory) {
    checkNotNull(transformerFactory);
    transformer = XmlTransformer.usingFactory(transformerFactory);
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a fop factory
   * @throws IOException iff an error occurs while reading the resources required by fop factory
   */
  private FopFactory getFopFactory(URI fopBaseUri) throws IOException {
    checkArgument(fopBaseUri.isAbsolute());
    final URL configUrl = DocBookTransformer.class.getResource("fop-config.xml");

    final FopFactory fopFactory;
    try (InputStream configStream = configUrl.openStream()) {
      fopFactory = FopFactory.newInstance(fopBaseUri, configStream);
    } catch (SAXException e) {
      throw new VerifyException(e);
    }
    verify(fopFactory.validateStrictly());
    verify(fopFactory.validateUserConfigStrictly());

    return fopFactory;
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a transformer
   * @throws IOException iff an error occurs while reading the fop factory required resources
   */
  public ToBytesTransformer usingBaseUri(URI fopBaseUri) throws IOException {
    final FopFactory fopFactory = getFopFactory(fopBaseUri);

    return new FoToBytesTransformer(fopFactory, transformer);
  }
}
