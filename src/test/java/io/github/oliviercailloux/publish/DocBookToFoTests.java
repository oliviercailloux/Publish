package io.github.oliviercailloux.publish;

import static io.github.oliviercailloux.publish.Resourcer.charSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import com.google.common.io.CharSource;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory.OutputProperties;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import nu.validator.htmlparser.dom.HtmlDocumentBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class DocBookToFoTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookToFoTests.class);

  public static void main(String[] args) throws Exception {
    final CharSource docBook = charSource("Overly long line.dbk");
    final XmlTransformerFactory helper =
        XmlTransformerFactory.usingFactory(KnownFactory.XALAN.factory());
    final CharSource myStyle = charSource("DocBook to Fo style.xsl");

    final String fo = helper.usingStylesheet(myStyle).charsToChars(docBook);
    Files.writeString(Path.of("out.fo"), fo);
  }

  private static void assertEqualsApartFromIds(XmlTransformerFactory factory,
      String expectedResource, Document fo) throws XmlException, IOException {
    LOGGER.debug("Stripping from expected.");
    Document expectedStripped = strippedDom(factory, expectedResource);
    LOGGER.debug("Removing ids from expected.");
    Document expectedWithoutIds = withoutIds(factory, expectedStripped);
    LOGGER.debug("Removing ids from FO.");
    Document foWithoutIds = withoutIds(factory, fo);
    DomHelper helper = DomHelper.domHelper();
    // Files.writeString(Path.of("Expected dom without ids.fo"), helper.toString(expectedWithoutIds));
    // Files.writeString(Path.of("Fo dom without ids.fo"), helper.toString(foWithoutIds));
    // assertTrue(expectedWithoutIds.isEqualNode(foWithoutIds));
    assertEquals(helper.toString(expectedWithoutIds), helper.toString(foWithoutIds));
  }

  private static Document strippedDom(XmlTransformerFactory factory, String name)
      throws XmlException, IOException {
    CharSource base = Resourcer.charSource(name);
    return factory.usingStylesheet(XmlTransformerFactory.STRIP_WHITESPACE_STYLESHEET,
        ImmutableMap.of(), OutputProperties.noIndent()).charsToDom(base);
  }

  private static Document withoutIds(XmlTransformerFactory factory, Document doc)
      throws XmlException, IOException {
    return factory.usingStylesheet(charSource("Support to Fo/Remove ids.xsl"), ImmutableMap.of(),
        OutputProperties.noIndent()).sourceToDom(new DOMSource(doc));
  }

  public String noIndent(XmlTransformerFactory factory, Document doc) throws IOException {
    return factory.usingStylesheet(XmlTransformerFactory.STRIP_WHITESPACE_STYLESHEET,
        ImmutableMap.of(), OutputProperties.noIndent()).sourceToChars(new DOMSource(doc));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple/Simple article.dbk");
    String name = "Simple/Simple article.fo";
    LOGGER.debug("Getting DOM helper.");
    DomHelper helper = DomHelper.domHelper();

    LOGGER.debug("Getting underlying factory.");
    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    /*
     * This is the slow part. With Saxon, the whole test takes 2683 ms, this part takes 2070 ms.
     * With Xalan, whole test 1059 ms, this part 818 ms.
     */
    LOGGER.debug("Getting transformer.");
    XmlTransformer transformer =
        transformerFactory.usingStylesheet(stylesheet, properties, OutputProperties.noIndent());
    LOGGER.debug("Transforming to FO.");
    Document foDom = transformer.charsToDom(docBook);
    LOGGER.debug("Transforming to String.");
    String fo = helper.toString(foDom);
    assertTrue(fo.contains("page-height=\"11in\""));
    assertTrue(fo.contains("page-width=\"8.5in\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    LOGGER.debug("Comparing.");
    assertEqualsApartFromIds(transformerFactory, name, foDom);
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoA4(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties =
        ImmutableMap.of(XmlName.localName("paper.type"), "A4");
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String fo = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoStyled(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("Support to Fo/DocBook to Fo style.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple/Simple article.dbk");
    String name = "Simple/Simple article styled.fo";

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final Document foDom = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToDom(docBook);
    String fo = DomHelper.domHelper().toString(foDom);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    assertEqualsApartFromIds(transformerFactory, name, foDom);
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoStyledImaged(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("Support to Fo/DocBook to Fo style with image.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String fo = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoStyledNonImage(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("Support to Fo/DocBook to Fo style with non existing image.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String fo = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    assertTrue(fo.contains(
        "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testArticleWithImageToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("With image/Article with image.dbk");
    String name = "With image/Article with image.fo";

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final Document foDom = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToDom(docBook);
    String fo = DomHelper.domHelper().toString(foDom);
    assertTrue(fo.contains("Sample"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    assertEqualsApartFromIds(transformerFactory, name, foDom);
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testArticleWithNonExistingImageToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("With image/Article with non existing image.dbk");
    String name = "With image/Article with non existing image.fo";

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final Document foDom = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToDom(docBook);
    String fo = DomHelper.domHelper().toString(foDom);
    assertTrue(fo.contains("Sample"));
    assertTrue(fo.contains(
        "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
    assertEqualsApartFromIds(transformerFactory, name, foDom);
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testHowtoToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Howto/Howto shortened.dbk");
    String name = "Howto/Howto shortened.fo";

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final Document foDom = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToDom(docBook);
    String fo = DomHelper.domHelper().toString(foDom);
    Files.writeString(Path.of("Howto shortened.fo"), fo);
    assertTrue(fo.contains("page-height=\"11in\""));
    assertTrue(fo.contains("page-width=\"8.5in\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("targeted at DocBook users"));
    assertEqualsApartFromIds(transformerFactory, name, foDom);
  }

  /**
   * Oddly enough, the fo processor accepts invalid DocBook instances. I didn’t investigate further.
   */
  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testHowtoInvalidToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Howto/Howto invalid.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    assertDoesNotThrow(() -> transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToHtml(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_HTML_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String html = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    assertFalse(html.contains("docbook.css"));
    final Element documentElement = DomHelper.usingBuilder(new HtmlDocumentBuilder())
        .asDocument(new StreamSource(new StringReader(html))).getDocumentElement();
    final ImmutableList<Element> titleElements =
        DomHelper.toElements(documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToHtmlChangeCss(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_HTML_URI;
    ImmutableMap<XmlName, String> properties =
        ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css");
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String html = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    assertTrue(html.contains("blah.css"));
    assertFalse(html.contains("docbook.css"));
    final Element documentElement = DomHelper.usingBuilder(new HtmlDocumentBuilder())
        .asDocument(new StreamSource(new StringReader(html))).getDocumentElement();
    final ImmutableList<Element> titleElements =
        DomHelper.toElements(documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToXhtml(KnownFactory factory) throws Exception {
    URI stylesheet = URI.create("http://cdn.docbook.org/release/xsl/1.79.2/xhtml/docbook.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String xhtml = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements =
        DomHelper.toElements(documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertFalse(xhtml.contains(".css"));
    assertEquals("My Article", titleElement.getTextContent());
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToXhtmlChangeCss(KnownFactory factory) throws Exception {
    URI stylesheet = URI.create("http://cdn.docbook.org/release/xsl/1.79.2/xhtml/docbook.xsl");
    ImmutableMap<XmlName, String> properties =
        ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css");
    // , XmlName.localName("docbook.css.source"), ""
    final CharSource docBook = charSource("Simple/Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory transformerFactory = XmlTransformerFactory.usingFactory(underlying);

    final String xhtml = transformerFactory
        .usingStylesheet(stylesheet, properties, OutputProperties.noIndent()).charsToChars(docBook);
    assertTrue(xhtml.contains("blah.css"));
    assertFalse(xhtml.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements =
        DomHelper.toElements(documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }
}
