package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

class DocBookTransformerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookTransformerTests.class);

  public static void main(String[] args) throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Overly long line.dbk").toString());
    final DocBookTransformer helper = DocBookTransformer.usingFactory(KnownFactory.XALAN.factory());
    final StreamSource myStyle = new StreamSource(
        DocBookTransformer.class.getResource("DocBook to Fo style.xsl").toString());

    final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
    Files.writeString(Path.of("out.fo"), fo);
  }

  @Test
  void testJdkFactoryFoThrows() throws Exception {
    final XmlTransformer t = XmlTransformer.usingFactory(KnownFactory.JDK.factory());
    assertThrows(XmlException.class, () -> t.forSource(DocBookTransformer.TO_FO_STYLESHEET));
  }

  @Test
  void testJdkFactoryHtmlThrows() throws Exception {
    final XmlTransformer t = XmlTransformer.usingFactory(KnownFactory.JDK.factory());
    assertThrows(XmlException.class, () -> t.forSource(DocBookTransformer.TO_HTML_STYLESHEET));
  }

  @Test
  void testJdkFactoryXhtmlThrows() throws Exception {
    final XmlTransformer t = XmlTransformer.usingFactory(KnownFactory.JDK.factory());
    assertThrows(XmlException.class, () -> t.forSource(DocBookTransformer.TO_XHTML_STYLESHEET));
  }

  @Test
  @Disabled("TODO")
  void testSimpleArticleToLocalXhtmlXalan() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());

    final StreamSource localStyle = new StreamSource(
        "file:///usr/share/xml/docbook/stylesheet/docbook-xsl-ns/xhtml5/docbook.xsl");

    final String xhtml = DocBookTransformer.usingFactory(KnownFactory.XALAN.factory())
        .usingStylesheet(localStyle).transform(docBook);
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
  @EnumSource(names = {"XALAN", "SAXON"})
  void testSimpleArticleToFo(KnownFactory factory) throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Simple article.dbk").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    {
      final String fo = helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Simple article using %s raw.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }

    {
      final String fo =
          helper.usingFoStylesheet(ImmutableMap.of(XmlName.localName("paper.type"), "A4"))
              .transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
    }

    {
      final StreamSource myStyle = new StreamSource(
          DocBookTransformer.class.getResource("DocBook to Fo style.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Simple article using %s styled.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }

    {
      final StreamSource myStyle = new StreamSource(
          DocBookTransformer.class.getResource("DocBook to Fo style with image.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    }

    {
      final StreamSource myStyle = new StreamSource(DocBookTransformer.class
          .getResource("DocBook to Fo style with non existing image.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
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
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Article with image.dbk").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    {
      final String fo = helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with image using %s raw.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }

    {
      final StreamSource myStyle = new StreamSource(
          DocBookTransformer.class.getResource("DocBook to Fo style.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with image using %s styled.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithSmallImageToFo(KnownFactory factory) throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Article with small image.dbk").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    final StreamSource myStyle = new StreamSource(
        DocBookTransformer.class.getResource("DocBook to Fo style.xsl").toString());
    final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
    assertTrue(fo.contains("Sample"));
    assertTrue(
        fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    final String expected = Files.readString(Path.of(DocBookTransformerTests.class
        .getResource("Article with small image using %s styled.fo".formatted(factory)).toURI()));
    assertEquals(expected, fo);
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testArticleWithNonExistingImageToFo(KnownFactory factory) throws Exception {
    final StreamSource docBook = new StreamSource(DocBookTransformerTests.class
        .getResource("Article with non existing image.dbk").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    {
      final String fo = helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with non existing image using %s raw.fo".formatted(factory))
          .toURI()));
      assertEquals(expected, fo);
    }

    {
      final StreamSource myStyle = new StreamSource(
          DocBookTransformer.class.getResource("DocBook to Fo style.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("Sample"));
      assertTrue(fo.contains(
          "https://github.com/Dauphine-MIDO/M1-alternance/raw/non-existent-branch/non-existent-graphic.png"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Article with non existing image using %s styled.fo".formatted(factory))
          .toURI()));
      assertEquals(expected, fo);
    }
  }

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testHowtoToFo(KnownFactory factory) throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Howto shortened.dbk").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    {
      final String fo = helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Howto shortened using %s raw.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }

    {
      final String fo =
          helper.usingFoStylesheet(ImmutableMap.of(XmlName.localName("paper.type"), "A4"))
              .transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
    }

    {
      final StreamSource myStyle = new StreamSource(
          DocBookTransformer.class.getResource("DocBook to Fo style.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      final String expected = Files.readString(Path.of(DocBookTransformerTests.class
          .getResource("Howto shortened using %s styled.fo".formatted(factory)).toURI()));
      assertEquals(expected, fo);
    }

    {
      final StreamSource myStyle = new StreamSource(
          DocBookTransformer.class.getResource("DocBook to Fo style with image.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("targeted at DocBook users"));
      assertTrue(
          fo.contains("https://github.com/Dauphine-MIDO/M1-alternance/raw/main/DauphineBleu.png"));
    }

    {
      final StreamSource myStyle = new StreamSource(DocBookTransformer.class
          .getResource("DocBook to Fo style with non existing image.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
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
    final StreamSource docBook =
        new StreamSource(DocBookTransformerTests.class.getResource("Howto invalid.dbk").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
    assertDoesNotThrow(() -> helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook));
  }

  @Test
  void testSimpleArticleToXhtmlXalan() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());
    /*
     * new StreamSource(
     * "file:///usr/share/xml/docbook/stylesheet/docbook-xsl-ns/xhtml5/docbook.xsl")
     */
    final String xhtml = DocBookTransformer.usingFactory(KnownFactory.XALAN.factory())
        .usingXhtmlStylesheet(ImmutableMap.of()).transform(docBook);
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
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());
    final String xhtml = DocBookTransformer.usingFactory(factory.factory())
        .usingXhtmlStylesheet(ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css",
            XmlName.localName("docbook.css.source"), ""))
        .transform(docBook);
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
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());
    final XmlException xmlExc = assertThrows(XmlException.class,
        () -> DocBookTransformer.usingFactory(KnownFactory.SAXON.factory())
            .usingXhtmlStylesheet(ImmutableMap.of()).transform(docBook));
    final String reason = xmlExc.getCause().getMessage();
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Can't make chunks with Saxonica's processor.",
        reason);
  }

  @Test
  void testSimpleArticleToXhtmlSaxonParamsSomeThrows() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());
    final XmlException xmlExc = assertThrows(XmlException.class,
        () -> DocBookTransformer.usingFactory(KnownFactory.SAXON.factory())
            .usingXhtmlStylesheet(ImmutableMap.of(XmlName.localName("html.stylesheet"), "blah.css"))
            .transform(docBook));
    final String reason = xmlExc.getCause().getMessage();
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Can't make chunks with Saxonica's processor.",
        reason);
  }

  @Test
  void testSimpleArticleToXhtmlSaxonParamsSomeDoesntThrow() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());
    assertDoesNotThrow(() -> DocBookTransformer.usingFactory(KnownFactory.SAXON.factory())
        .usingXhtmlStylesheet(ImmutableMap.of(XmlName.localName("docbook.css.source"), ""))
        .transform(docBook));
  }
}
