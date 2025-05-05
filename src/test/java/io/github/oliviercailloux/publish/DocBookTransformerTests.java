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
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory.OutputProperties;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

class DocBookTransformerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookTransformerTests.class);

  private static final Pattern IDS_PATTERN = Pattern.compile("id=\".+\"");

  private static final Pattern REF_PATTERN = Pattern.compile("internal-destination=\".+\"");

  public static void main(String[] args) throws Exception {
    final CharSource docBook = charSource("Overly long line.dbk");
    final XmlTransformerFactory helper =
        XmlTransformerFactory.usingFactory(KnownFactory.XALAN.factory());
    final CharSource myStyle = charSource("DocBook to Fo style.xsl");

    final String fo = helper.usingStylesheet(myStyle).charsToChars(docBook);
    Files.writeString(Path.of("out.fo"), fo);
  }

  private static String withoutIds(String original) {
    final String withoutBlockIds = IDS_PATTERN.matcher(original).replaceAll(r -> "id=\"\"");
    final String withoutIds =
        REF_PATTERN.matcher(withoutBlockIds).replaceAll(r -> "internal-destination=\"\"");
    return withoutIds;
  }

  private static void assertEqualsApartFromIds(String expected, String fo) {
    assertEquals(withoutIds(expected), withoutIds(fo));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple article.dbk");
    String name = "Simple article using %s.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"11in\""));
    assertTrue(fo.contains("page-width=\"8.5in\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    assertEquals(Resourcer.charSource(name).read(), fo);
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoA4(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties =
        ImmutableMap.of(XmlName.localName("paper.type"), "A4");
    final CharSource docBook = charSource("Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoStyled(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("DocBook to Fo style.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple article.dbk");
    String name = "Simple article using %s styled.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    assertEqualsApartFromIds(Resourcer.charSource(name).read(), fo);
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFoStyledImaged(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("DocBook to Fo style with image.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
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
    CharSource stylesheet = charSource("DocBook to Fo style with non existing image.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("page-height=\"297mm\""));
    assertTrue(fo.contains("page-width=\"210mm\""));
    assertTrue(fo.contains("<fo:block"));
    assertTrue(fo.contains("On the Possibility of Going Home"));
    assertTrue(fo.contains(
        "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithImageToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Article with image.dbk");
    String name = "Article with image using %s raw.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("Sample"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    assertEqualsApartFromIds(Resourcer.charSource(name).read(), fo);
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithImageToFoStyled(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("DocBook to Fo style.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Article with image.dbk");
    String name = "Article with image using %s styled.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("Sample"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    assertEqualsApartFromIds(Resourcer.charSource(name).read(), fo);
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithSmallImageToFoStyled(KnownFactory factory) throws Exception {
    CharSource stylesheet = charSource("DocBook to Fo style.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Article with small image.dbk");
    String name = "Article with small image using %s styled.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("Sample"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    assertEqualsApartFromIds(Resourcer.charSource(name).read(), fo);
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithNonExistingImageToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Article with non existing image.dbk");
    // String name = "Article with non existing image using %s raw.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
    assertTrue(fo.contains("Sample"));
    assertTrue(fo.contains(
        "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
    // assertEqualsApartFromIds(Resourcer.charSource(name).read(), fo);
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testHowtoToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Howto shortened.dbk");
    String name = "Howto shortened using %s raw.fo".formatted(factory);

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

        final String fo = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      Files.writeString(Path.of(name), fo);
      assertEqualsApartFromIds(Resourcer.charSource(name).read(), fo);
  }

  /**
   * Oddly enough, the fo processor accepts invalid DocBook instances. I didn’t investigate further.
   */
  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testHowtoInvalidToFo(KnownFactory factory) throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_FO_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Howto invalid.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    assertDoesNotThrow(
        () -> helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
        .charsToChars(docBook));
  }

  @Test
  void testSimpleArticleToHtmlXalanTodo() throws Exception {
    URI stylesheet = DocBookResources.XSLT_1_HTML_URI;
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple article.dbk");

    TransformerFactory underlying = KnownFactory.XALAN.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String html = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
    .charsToChars(docBook);
    Files.writeString(Path.of("Simple article using Xalan.html"), html);
    // assertTrue(html.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(html))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testSimpleArticleToXhtml(KnownFactory factory) throws Exception {
    URI stylesheet = URI.create("http://cdn.docbook.org/release/xsl/1.79.2/xhtml/docbook.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of();
    final CharSource docBook = charSource("Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String xhtml = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
    .charsToChars(docBook);
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertFalse(xhtml.contains(".css"));
    assertEquals("My Article", titleElement.getTextContent());
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testSimpleArticleToXhtmlChangeCss(KnownFactory factory) throws Exception {
    URI stylesheet = URI.create("http://cdn.docbook.org/release/xsl/1.79.2/xhtml/docbook.xsl");
    ImmutableMap<XmlName, String> properties = ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css",
    XmlName.localName("docbook.css.source"), "");
    final CharSource docBook = charSource("Simple article.dbk");

    TransformerFactory underlying = factory.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(underlying);

    final String xhtml = helper.usingStylesheet(stylesheet, properties, OutputProperties.noIndent())
    .charsToChars(docBook);
    // Files.writeString(Path.of("Simple article using %s.html".formatted(factory)), xhtml);
    assertTrue(xhtml.contains("blah.css"));
    assertTrue(!xhtml.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagName("title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }
}
