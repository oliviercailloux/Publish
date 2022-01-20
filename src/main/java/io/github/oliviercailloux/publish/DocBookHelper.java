package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.SchemaHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.model.EventSeverity;
import org.apache.xmlgraphics.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The public API of this class favors {@link StreamSource} (from {@code javax.xml.transform}) to
 * {@link InputSource} (from {@code org.xml.sax}). The documentation of
 * {@link io.github.oliviercailloux.jaris.xml} provides some rationale.
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/XSL_Formatting_Objects">XSL FO</a>.
 * </p>
 */
public class DocBookHelper {
  public static final StreamSource TO_FO_STYLESHEET =
      new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");
  public static final StreamSource TO_HTML_STYLESHEET =
      new StreamSource("https://cdn.docbook.org/release/xsl/current/html/docbook.xsl");
  public static final StreamSource TO_XHTML_STYLESHEET =
      new StreamSource("https://cdn.docbook.org/release/xsl/current/xhtml5/docbook.xsl");
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookHelper.class);

  public static DocBookHelper usingDefaultFactory() {
    return new DocBookHelper(new org.apache.xalan.processor.TransformerFactoryImpl());
  }

  public static DocBookHelper usingFactory(TransformerFactory factory) {
    return new DocBookHelper(factory);
  }

  private final TransformerFactory factory;
  private ConformityChecker conformityChecker;
  private XmlTransformer transformer;

  private DocBookHelper(TransformerFactory transformerFactory) {
    factory = checkNotNull(transformerFactory);
    conformityChecker = null;
    transformer = null;
  }

  private ConformityChecker lazyGetConformityChecker() {
    if (conformityChecker == null) {
      final StreamSource schemaSource =
          new StreamSource(DocBookHelper.class.getResource("docbook.rng").toString());
      final SchemaFactory schemaFactory = new XMLSyntaxSchemaFactory();
      final SchemaHelper schemaHelper = SchemaHelper.schemaHelper(schemaFactory);
      conformityChecker = schemaHelper.getConformityChecker(schemaSource);
    }
    return conformityChecker;
  }

  private XmlTransformer lazyGetTransformer() {
    if (transformer == null) {
      transformer = XmlTransformer.usingFactory(factory);
    }
    return transformer;
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a fop factory
   * @throws IOException iff an error occurs while reading the resources required by fop factory
   */
  private FopFactory getFopFactory(URI fopBaseUri) throws IOException {
    checkArgument(fopBaseUri.isAbsolute());
    final URL configUrl = DocBookHelper.class.getResource("fop-config.xml");

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
   * Throws an exception iff the given source is not a valid DocBook.
   *
   * @param docBook the DocBook to validate
   * @throws VerifyException iff the document is invalid, equivalently, iff a warning, error or
   *         fatalError is encountered while validating the provided document
   * @throws XmlException if the Source is an XML artifact that the implementation cannot validate
   *         (for example, a processing instruction)
   * @throws IOException if the validator is processing a javax.xml.transform.sax.SAXSource and the
   *         underlying org.xml.sax.XMLReader throws an IOException.
   */
  public void verifyValid(Source docBook) throws VerifyException, XmlException, IOException {
    lazyGetConformityChecker().verifyValid(docBook);
  }

  /**
   * @param docBook the source
   * @param stylesheet for example, use {@link #TO_FO_STYLESHEET} to transform the provided source
   *        into an FO format using a default DocBook to FO stylesheet.
   * @return the transformed content as a string
   * @throws XmlException iff an error occurs when parsing the stylesheet or when transforming the
   *         document.
   */
  public String docBookTo(Source docBook, Source stylesheet) {
    checkArgument(!docBook.isEmpty());
    checkArgument(!stylesheet.isEmpty());
    return lazyGetTransformer().transform(docBook, stylesheet);
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @param fo the XSL FO source, not empty
   * @param pdfStream the stream where the resulting pdf will be output
   * @throws IOException iff an error occurs while reading the fop factory required resources
   * @throws XmlException iff an error occurs when transforming the document
   */
  public void foToPdf(URI fopBaseUri, Source fo, OutputStream pdfStream)
      throws IOException, XmlException {
    final FopFactory fopFactory = getFopFactory(fopBaseUri);

    final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
    foUserAgent.getEventBroadcaster().addEventListener((e) -> {
      /* https://xmlgraphics.apache.org/fop/2.4/events.html */
      if (!e.getSeverity().equals(EventSeverity.INFO)) {
        throw new XmlException(e.toString());
      }
    });

    final Result res;
    try {
      final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, pdfStream);
      res = new SAXResult(fop.getDefaultHandler());
    } catch (FOPException e) {
      throw new IllegalStateException(e);
    }

    lazyGetTransformer().transform(fo, res);
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @param docBook not empty.
   * @param stylesheet the default {@link #TO_FO_STYLESHEET} or another suitable one.
   * @param pdfStream the stream where the resulting pdf will be output
   * @throws IOException iff an error occurs while reading the fop factory required resources
   * @throws XmlException iff an error occurs when parsing the stylesheet or when transforming the
   *         document.
   */
  public void docBookToPdf(URI fopBaseUri, Source docBook, Source stylesheet,
      OutputStream pdfStream) throws IOException, XmlException {
    checkArgument(!stylesheet.isEmpty());
    LOGGER.info("Converting to Fop.");
    final String fo = docBookTo(docBook, stylesheet);
    final StreamSource foSource = new StreamSource(new StringReader(fo));
    LOGGER.info("Writing PDF.");
    foToPdf(fopBaseUri, foSource, pdfStream);
  }
}
