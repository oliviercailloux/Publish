package io.github.oliviercailloux.publish;

import static io.github.oliviercailloux.publish.Resourcer.charSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import com.google.common.io.CharSource;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory.OutputProperties;
import io.github.oliviercailloux.testutils.OutputCapturer;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
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

  @Test
  void testJdkFactoryFoThrows() throws Exception {
    final OutputCapturer capturer = OutputCapturer.capturer();
    capturer.capture();
    final XmlTransformerFactory t = XmlTransformerFactory.usingFactory(KnownFactory.JDK.factory());
    assertThrows(XmlException.class, () -> t.usingStylesheet(DocBookStylesheets.TO_FO_STYLESHEET));
    capturer.restore();
    assertTrue(capturer.out().isEmpty());
    assertTrue(capturer.err().lines().count() > 100);
  }

  @Test
  void testJdkFactoryHtmlThrows() throws Exception {
    final OutputCapturer capturer = OutputCapturer.capturer();
    capturer.capture();
    final XmlTransformerFactory t = XmlTransformerFactory.usingFactory(KnownFactory.JDK.factory());
    assertThrows(XmlException.class,
        () -> t.usingStylesheet(DocBookStylesheets.TO_HTML_STYLESHEET));
    capturer.restore();
    assertTrue(capturer.out().isEmpty());
    assertTrue(capturer.err().lines().count() > 100);
  }

  @Test
  void testJdkFactoryXhtmlThrows() throws Exception {
    final OutputCapturer capturer = OutputCapturer.capturer();
    capturer.capture();
    final XmlTransformerFactory t = XmlTransformerFactory.usingFactory(KnownFactory.JDK.factory());
    assertThrows(XmlException.class,
        () -> t.usingStylesheet(DocBookStylesheets.TO_XHTML_STYLESHEET));
    capturer.restore();
    assertTrue(capturer.out().isEmpty());
    assertTrue(capturer.err().lines().count() > 100);
  }

  /**
   * docbook-xsl-ns (1.79.2+dfsg-1) https://packages.debian.org/bullseye/docbook-xsl-ns
   *
   * @throws Exception
   */
  @Test
  void testSimpleArticleToCpXhtmlXalan() throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");

    // final CharSource localStyle = charSource("docbook-xsl-ns/xhtml5/docbook.xsl");
    final StreamSource localStyle = new StreamSource(
        DocBookConformityChecker.class.getResource("docbook-xsl-ns/xhtml5/docbook.xsl").toString());
    // final CharSource localStyle =
    // charSource(DocBookConformityChecker.class.getResource("docbook-xsl-ns/xhtml5/docbook.xsl"));

    // LOGGER.debug("Using style {}.",
    // Files.readString(Path.of(new URL(localStyle.getSystemId()).toURI())));

    final String xhtml = XmlTransformerFactory.usingFactory(KnownFactory.XALAN.factory())
        .usingStylesheet(localStyle, ImmutableMap.of(), OutputProperties.indent())
        .charsToChars(docBook);
    LOGGER.debug("Resulting XHTML: {}.", xhtml);
    assertTrue(xhtml.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagNameNS(DomHelper.HTML_NS_URI.toString(), "title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
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
    final CharSource docBook = charSource("Simple article.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());
    final StreamSource foStylesheet =
        new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");

    {
      final String fo =
          helper.usingStylesheet(foStylesheet, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      // Files.writeString(Path.of("Simple article using %s raw indented.fo".formatted(factory)),
      // fo);
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Simple article using %s raw.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }

    {
      final String fo = helper.usingStylesheet(foStylesheet,
          ImmutableMap.of(XmlName.localName("paper.type"), "A4"), OutputProperties.noIndent())
          .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Simple article using %s styled.fo".formatted(factory)).toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style with image.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style with non existing image.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
    }
  }

  @ParameterizedTest
  @EnumSource(names = {"SAXON"})
  void testSimpleArticleToFoCp(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());
    final StreamSource foStylesheet =
    new StreamSource(
      DocBookConformityChecker.class.getResource("docbook-xsl-ns/fo/docbook.xsl").toString());

    {
      final String fo =
          helper.usingStylesheet(foStylesheet, ImmutableMap.of(), OutputProperties.indent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      /*
       * Minor differences between the classpath and the online styles, such as text-align="start"
       * VS "left".
       */
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Simple article using %s raw classpath indented.fo".formatted(factory))
          .toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final String fo = helper.usingStylesheet(foStylesheet,
          ImmutableMap.of(XmlName.localName("paper.type"), "A4"), OutputProperties.noIndent())
          .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
    }

    /* FIXME This is not using CP! Need to figure out how to change this. */
    {
      final CharSource myStyle = charSource("DocBook to Fo style.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Simple article using %s styled.fo".formatted(factory)).toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style with image.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style with non existing image.xsl");
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
    }
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithImageToFo(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Article with image.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());
    final StreamSource foStylesheet =
        new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");

    {
      final String fo =
          helper.usingStylesheet(foStylesheet, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with image using %s raw.fo".formatted(factory)).toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with image using %s styled.fo".formatted(factory)).toURI()));
      assertEqualsApartFromIds(expected, fo);
    }
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithSmallImageToFo(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Article with small image.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());

    final CharSource myStyle = charSource("DocBook to Fo style.xsl");
    final String fo =
        helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
            .charsToChars(docBook);
    assertTrue(fo.contains("Sample"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    final String expected = Files.readString(Path.of(DocBookTransformerTests.class
        .getResource("Article with small image using %s styled.fo".formatted(factory)).toURI()));
    assertEqualsApartFromIds(expected, fo);
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithNonExistingImageToFo(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Article with non existing image.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());
    final StreamSource foStylesheet =
        new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");

    {
      final String fo =
          helper.usingStylesheet(foStylesheet, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with non existing image using %s raw.fo".formatted(factory))
          .toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with non existing image using %s styled.fo".formatted(factory))
          .toURI()));
      assertEqualsApartFromIds(expected, fo);
    }
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testHowtoToFo(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Howto shortened.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());
    final StreamSource foStylesheet =
        new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");

    {
      final String fo =
          helper.usingStylesheet(foStylesheet, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Howto shortened using %s raw.fo".formatted(factory)).toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final String fo = helper.usingStylesheet(foStylesheet,
          ImmutableMap.of(XmlName.localName("paper.type"), "A4"), OutputProperties.noIndent())
          .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Howto shortened using %s styled.fo".formatted(factory)).toURI()));
      assertEqualsApartFromIds(expected, fo);
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style with image.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    }

    {
      final CharSource myStyle = charSource("DocBook to Fo style with non existing image.xsl");
      final String fo =
          helper.usingStylesheet(myStyle, ImmutableMap.of(), OutputProperties.noIndent())
              .charsToChars(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
    }
  }

  /**
   * Oddly enough, the fo processor accepts invalid DocBook instances. I didn’t investigate further.
   */
  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testHowtoInvalidToFo(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Howto invalid.dbk");

    final XmlTransformerFactory helper = XmlTransformerFactory.usingFactory(factory.factory());
    final StreamSource foStylesheet =
        new StreamSource("https://cdn.docbook.org/release/xsl/current/fo/docbook.xsl");

    helper.usingStylesheet(DocBookStylesheets.TO_FO_STYLESHEET, ImmutableMap.of())
        .charsToChars(docBook);
    assertDoesNotThrow(
        () -> helper.usingStylesheet(foStylesheet, ImmutableMap.of(), OutputProperties.noIndent())
            .charsToChars(docBook));
  }

  @Test
  void testSimpleArticleToXhtmlXalan() throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");
    /*
     * new StreamSource(
     * "file:///usr/share/xml/docbook/stylesheet/docbook-xsl-ns/xhtml5/docbook.xsl")
     */
    final String xhtml = XmlTransformerFactory.usingFactory(KnownFactory.XALAN.factory())
        .usingStylesheet(DocBookStylesheets.TO_XHTML_STYLESHEET, ImmutableMap.of())
        .charsToChars(docBook);
    LOGGER.debug("Resulting XHTML: {}.", xhtml);
    assertTrue(xhtml.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagNameNS(DomHelper.HTML_NS_URI.toString(), "title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testSimpleArticleToXhtmlChangeCss(KnownFactory factory) throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");
    final String xhtml = XmlTransformerFactory.usingFactory(factory.factory())
        .usingStylesheet(DocBookStylesheets.TO_XHTML_STYLESHEET,
            ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css",
                XmlName.localName("docbook.css.source"), ""))
        .charsToChars(docBook);
    LOGGER.debug("Resulting XHTML: {}.", xhtml);
    assertTrue(xhtml.contains("blah.css"));
    assertTrue(!xhtml.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagNameNS(DomHelper.HTML_NS_URI.toString(), "title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }

  /**
   * Oddly enough, Saxon fails when not parameterized, but succeeds with some parameters.
   */
  @Test
  void testSimpleArticleToXhtmlSaxonParamsNoneThrows() throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");
    final XmlException xmlExc = assertThrows(XmlException.class,
        () -> XmlTransformerFactory.usingFactory(KnownFactory.SAXON.factory())
            .usingStylesheet(DocBookStylesheets.TO_XHTML_STYLESHEET, ImmutableMap.of())
            .charsToChars(docBook));
    final String reason = xmlExc.getCause().getMessage();
    assertEquals(
        "Processing terminated by xsl:message at line 239 in chunker.xsl",
        reason);
    /* Indeed, logs “Can't make chunks with Saxonica's processor”, and https://cdn.docbook.org/release/xsl/current/xhtml/chunker.xsl has xsl:message terminate="yes" with that message. */
  }

  @Test
  void testSimpleArticleToXhtmlSaxonParamsSomeThrows() throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");
    final XmlException xmlExc = assertThrows(XmlException.class,
        () -> XmlTransformerFactory.usingFactory(KnownFactory.SAXON.factory())
            .usingStylesheet(DocBookStylesheets.TO_XHTML_STYLESHEET,
                ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css"))
            .charsToChars(docBook));
    final String reason = xmlExc.getCause().getMessage();
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Can't make chunks with Saxonica's processor.",
        reason);
  }

  @Test
  void testSimpleArticleToXhtmlSaxonParamsSomeDoesntThrow() throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");
    assertDoesNotThrow(
        () -> XmlTransformerFactory.usingFactory(KnownFactory.SAXON.factory())
            .usingStylesheet(DocBookStylesheets.TO_XHTML_STYLESHEET,
                ImmutableMap.of(XmlName.localName("docbook.css.source"), ""))
            .charsToChars(docBook));
  }
}
