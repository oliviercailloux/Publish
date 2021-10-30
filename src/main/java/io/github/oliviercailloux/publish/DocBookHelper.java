package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import io.github.oliviercailloux.jaris.xml.XmlUtils;
import io.github.oliviercailloux.jaris.xml.XmlUtils.SchemaHelper;
import io.github.oliviercailloux.jaris.xml.XmlUtils.Transformer;
import io.github.oliviercailloux.jaris.xml.XmlUtils.XmlException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import net.sf.saxon.TransformerFactoryImpl;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.hyphenation.Hyphenator;
import org.apache.xmlgraphics.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The public API of this class favors {@link StreamSource} (from {@code javax.xml.transform}) to
 * {@link InputSource} (from {@code org.xml.sax}). The documentation of {@link XmlUtils} provides
 * some rationale.
 */
public class DocBookHelper {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookHelper.class);

  public static DocBookHelper instance() {
    return new DocBookHelper();
  }

  private XmlUtils.SchemaHelper validator;
  private Transformer transformer;
  private FopFactory fopFactory;

  private DocBookHelper() {
    validator = null;
    transformer = null;
    fopFactory = null;
  }

  private SchemaHelper lazyGetValidator() {
    final StreamSource schemaSource =
        new StreamSource(DocBookHelper.class.getResource("docbook.rng").toString());
    final SchemaFactory schemaFactory = new XMLSyntaxSchemaFactory();
    validator = XmlUtils.schemaHelper(schemaFactory);
    validator.setSchema(schemaSource);
    return validator;
  }

  private Transformer lazyGetTransformer() {
    transformer = XmlUtils.transformer(new TransformerFactoryImpl());
    return transformer;
  }

  /**
   * @return a fop factory
   * @throws IOException iff an error occurs while reading the resources required by fop factory
   */
  private FopFactory lazyGetFopFactory() throws IOException {
    if (fopFactory == null) {
      final URL configUrl = DocBookHelper.class.getResource("fop-config.xml");
      final URI hyphenatorUri = Unchecker.URI_UNCHECKER
          .getUsing(() -> Hyphenator.class.getResource("en.hyp").toURI().resolve(""));

      try (InputStream configStream = configUrl.openStream()) {
        fopFactory = FopFactory.newInstance(hyphenatorUri, configStream);
      } catch (SAXException e) {
        throw new VerifyException(e);
      }
      verify(fopFactory.validateStrictly());
      verify(fopFactory.validateUserConfigStrictly());
    }
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
    lazyGetValidator().verifyValid(docBook);
  }

  /**
   * @param docBook not empty.
   * @param stylesheet if empty, a default stylesheet will be used.
   * @return the fop as a string
   * @throws XmlException iff an error occurs when parsing the stylesheet or when transforming the
   *         document.
   */
  public String docBookToFop(Source docBook, Source stylesheet) throws XmlException {
    checkArgument(!docBook.isEmpty());
    final Source styleSource;
    if (stylesheet.isEmpty()) {
      styleSource = new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");
    } else {
      styleSource = stylesheet;
    }
    return lazyGetTransformer().transform(docBook, styleSource);
  }

  /**
   * @param fo not empty
   * @param pdfStream the stream where the resulting pdf will be output
   * @throws IOException iff an error occurs while reading the fop factory required resources
   * @throws XmlException iff an error occurs when transforming the document
   */
  public void foToPdf(Source fo, OutputStream pdfStream) throws IOException, XmlException {
    lazyGetFopFactory();

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
   * @param docBook not empty.
   * @param style if empty, a default style will be used.
   * @param pdfStream the stream where the resulting pdf will be output
   * @throws IOException iff an error occurs while reading the fop factory required resources
   * @throws XmlException iff an error occurs when parsing the stylesheet or when transforming the
   *         document.
   */
  public void docBookToPdf(Source docBook, Source style, OutputStream pdfStream)
      throws IOException, XmlException {
    LOGGER.info("Converting to Fop.");
    final String fop = docBookToFop(docBook, style);
    final StreamSource fopSource = XmlUtils.asSource(fop);
    LOGGER.info("Writing PDF.");
    foToPdf(fopSource, pdfStream);
  }
}
