package io.github.oliviercailloux.publish;

import io.github.oliviercailloux.jaris.xml.XmlConfiguredTransformer;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformer.OutputProperties;
import java.util.Map;
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

  /**
   * Provides a transformer instance using the provided factory.
   *
   * @param factory I recommend using the XALAN or SAXON factory; the JDK embedded one fails on
   *        multiple DocBook stylesheets.
   * @return a doc book transformer
   */
  public static DocBookTransformer usingFactory(TransformerFactory factory) {
    /*
     * Note that using a pedantic factory (i.e., failing upon encountering warning messages) is
     * hopeless, as DocBook spits out messages such as “Making portrait pages on A4 paper
     * (210mmx297mm)”.
     */
    return new DocBookTransformer(XmlTransformer.usingFactory(factory));
  }

  private final XmlTransformer transformer;

  private DocBookTransformer(XmlTransformer transformer) {
    this.transformer = transformer;
  }

  /**
   * Returns a sourced transformer that may be used to transform documents using the provided
   * stylesheet and a default output property {@link OutputProperties#INDENT}.
   *
   * @param stylesheet for example, use {@link #TO_XHTML_STYLESHEET} to use a default DocBook to
   *        XHTML stylesheet.
   * @return a transformer
   * @throws XmlException iff an error occurs when parsing the stylesheet.
   */
  public XmlConfiguredTransformer usingStylesheet(Source stylesheet) throws XmlException {
    return transformer.usingStylesheet(stylesheet);
  }

  /**
   * Returns a sourced transformer that may be used to transform documents using the provided
   * stylesheet and a default output property {@link OutputProperties#INDENT}.
   *
   * @param stylesheet for example, use {@link #TO_XHTML_STYLESHEET} to use a default DocBook to
   *        XHTML stylesheet
   * @param parameters to use together with the stylesheet
   * @return a transformer
   * @throws XmlException iff an error occurs when parsing the stylesheet.
   */
  public XmlConfiguredTransformer usingStylesheet(Source stylesheet,
      Map<XmlName, String> parameters) {
    return transformer.usingStylesheet(stylesheet, parameters);
  }

  /**
   * Returns a sourced transformer that may be used to transform documents using the provided
   * stylesheet.
   *
   * @param stylesheet for example, use {@link #TO_XHTML_STYLESHEET} to use a default DocBook to
   *        XHTML stylesheet
   * @param parameters to use together with the stylesheet
   * @param outputProperties any properties to be used with the transformer.
   * @return a transformer
   * @throws XmlException iff an error occurs when parsing the stylesheet.
   */
  public XmlConfiguredTransformer usingStylesheet(Source stylesheet,
      Map<XmlName, String> parameters, OutputProperties outputProperties) {
    return transformer.usingStylesheet(stylesheet, parameters, outputProperties);
  }

  public XmlConfiguredTransformer usingFoStylesheet(Map<XmlName, String> parameters) {
    return transformer.usingStylesheet(TO_FO_STYLESHEET, parameters);
  }

  public XmlConfiguredTransformer usingHtmlStylesheet(Map<XmlName, String> parameters) {
    return transformer.usingStylesheet(TO_HTML_STYLESHEET, parameters);
  }

  public XmlConfiguredTransformer usingXhtmlStylesheet(Map<XmlName, String> parameters) {
    return transformer.usingStylesheet(TO_XHTML_STYLESHEET, parameters);
  }
}
