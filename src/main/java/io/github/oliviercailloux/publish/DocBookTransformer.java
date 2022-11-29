package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkNotNull;

import io.github.oliviercailloux.jaris.xml.XmlConfiguredTransformer;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Map;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * The public API of this class favors {@link StreamSource} (from {@code javax.xml.transform}) to
 * {@link InputSource} (from {@code org.xml.sax}). The documentation of
 * {@link io.github.oliviercailloux.jaris.xml} provides some rationale.
 * <p>
 * See <a href="https://en.wikipedia.org/wiki/XSL_Formatting_Objects">XSL FO</a>.
 * </p>
 */
public class DocBookTransformer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookTransformer.class);

  public static final StreamSource TO_FO_STYLESHEET =
      new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");
  public static final StreamSource TO_HTML_STYLESHEET =
      new StreamSource("https://cdn.docbook.org/release/xsl/current/html/docbook.xsl");
  public static final StreamSource TO_XHTML_STYLESHEET =
      new StreamSource("https://cdn.docbook.org/release/xsl/current/xhtml5/docbook.xsl");

  public static interface DocBookToFoTransformer extends XmlConfiguredTransformer {
    /**
     * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
     */
    ToBytesTransformer asDocBookToPdfTransformer(URI fopBaseUri) throws IOException;
  }

  private static class DocBookToFoTransformerFromXmlTransformer implements DocBookToFoTransformer {
    private final TransformerFactory transformerFactory;
    private final XmlConfiguredTransformer docBookToFoTransformer;

    private DocBookToFoTransformerFromXmlTransformer(TransformerFactory transformerFactory,
        XmlConfiguredTransformer docBookToFoTransformer) {
      this.transformerFactory = checkNotNull(transformerFactory);
      this.docBookToFoTransformer = checkNotNull(docBookToFoTransformer);
    }

    @Override
    public void transform(Source document, Result result) throws XmlException {
      docBookToFoTransformer.transform(document, result);
    }

    @Override
    public String transform(Source document) throws XmlException {
      return docBookToFoTransformer.transform(document);
    }

    @Override
    public ToBytesTransformer asDocBookToPdfTransformer(URI fopBaseUri) throws IOException {
      final ToBytesTransformer foToPdfTransformer =
          FoToPdfTransformer.usingFactory(transformerFactory).usingBaseUri(fopBaseUri);
      return new ToBytesFromXmlTransformer(docBookToFoTransformer, foToPdfTransformer);
    }
  }

  private static class ToBytesFromXmlTransformer implements ToBytesTransformer {
    private final XmlConfiguredTransformer docBookToFoTransformer;

    private final ToBytesTransformer foToPdfTransformer;

    private ToBytesFromXmlTransformer(XmlConfiguredTransformer docBookToFoTransformer,
        ToBytesTransformer foToPdfTransformer) {
      this.docBookToFoTransformer = checkNotNull(docBookToFoTransformer);
      this.foToPdfTransformer = checkNotNull(foToPdfTransformer);
    }

    @Override
    public void toStream(Source docBook, OutputStream pdfStream) throws IOException, XmlException {
      LOGGER.info("Converting to Fop.");
      final String fo = docBookToFoTransformer.transform(docBook);
      final StreamSource foSource = new StreamSource(new StringReader(fo));
      LOGGER.info("Writing PDF.");
      foToPdfTransformer.toStream(foSource, pdfStream);
    }
  }

  public static DocBookTransformer usingDefaultFactory() {
    return new DocBookTransformer(new org.apache.xalan.processor.TransformerFactoryImpl());
  }

  public static DocBookTransformer usingFactory(TransformerFactory factory) {
    return new DocBookTransformer(factory);
  }

  private final TransformerFactory transformerFactory;

  private final XmlTransformer transformer;

  private DocBookTransformer(TransformerFactory transformerFactory) {
    this.transformerFactory = checkNotNull(transformerFactory);
    transformer = XmlTransformer.usingFactory(transformerFactory);
  }

  /**
   * @param stylesheet for example, use {@link #TO_XHTML_STYLESHEET} to use a default DocBook to
   *        XHTML stylesheet.
   * @return a transformer
   * @throws XmlException iff an error occurs when parsing the stylesheet.
   */
  public XmlConfiguredTransformer usingStylesheet(Source stylesheet) throws XmlException {
    return transformer.forSource(stylesheet);
  }

  /**
   * @param stylesheet for example, use {@link #TO_XHTML_STYLESHEET} to use a default DocBook to
   *        XHTML stylesheet
   * @param parameters to use together with the stylesheet
   * @return a transformer
   * @throws XmlException iff an error occurs when parsing the stylesheet.
   */
  public XmlConfiguredTransformer usingStylesheet(Source stylesheet,
      Map<XmlName, String> parameters) {
    return transformer.forSource(stylesheet, parameters);
  }

  public DocBookToFoTransformer usingFoStylesheet(Map<XmlName, String> parameters) {
    return usingFoStylesheet(TO_FO_STYLESHEET, parameters);
  }

  public DocBookToFoTransformer usingFoStylesheet(Source stylesheet,
      Map<XmlName, String> parameters) throws XmlException {
    final XmlConfiguredTransformer docBookToFoTransformer =
        transformer.forSource(stylesheet, parameters);
    return new DocBookToFoTransformerFromXmlTransformer(transformerFactory, docBookToFoTransformer);
  }

  public XmlConfiguredTransformer usingHtmlStylesheet(Map<XmlName, String> parameters) {
    return transformer.forSource(TO_HTML_STYLESHEET, parameters);
  }

  public XmlConfiguredTransformer usingXhtmlStylesheet(Map<XmlName, String> parameters) {
    return transformer.forSource(TO_XHTML_STYLESHEET, parameters);
  }
}
