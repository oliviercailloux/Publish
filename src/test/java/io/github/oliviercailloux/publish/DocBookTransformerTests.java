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

  @ParameterizedTest
  @EnumSource(value = KnownFactory.class, names = {"XALAN", "SAXON"})
  void testSimpleArticleToFo(KnownFactory factory) throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());

    final DocBookTransformer helper = DocBookTransformer.usingFactory(factory.factory());

    {
      final String fo = helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
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
      final StreamSource myStyle =
          new StreamSource(DocBookTransformer.class.getResource("mystyle.xsl").toString());
      final String fo = helper.usingStylesheet(myStyle, ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
    }
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
