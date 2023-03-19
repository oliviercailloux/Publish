package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.nio.file.Path;
import java.time.Instant;
import javax.xml.transform.stream.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

class DocBookHelperTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookHelperTests.class);

  @Test
  void testDocBookValid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto shortened.xml").toString());
    assertDoesNotThrow(() -> DocBookConformityChecker.usingDefaults().verifyValid(docBook));
  }

  @Test
  void testDocBookInvalid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto invalid.xml").toString());
    assertThrows(VerifyException.class,
        () -> DocBookConformityChecker.usingDefaults().verifyValid(docBook));
  }

  @Test
  void testDocBookSimpleArticleToFo() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article.xml").toString());
    DocBookConformityChecker.usingDefaults().verifyValid(docBook);

    final DocBookTransformer helper =
        DocBookTransformer.usingFactory(new org.apache.xalan.processor.TransformerFactoryImpl());

    {
      final String fo = helper.usingFoStylesheet(ImmutableMap.of()).transform(docBook);
      assertTrue(fo.contains("page-height=\"11in\""));
      assertTrue(fo.contains("page-width=\"8.5in\""));
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

    {
      final String fo =
          helper.usingFoStylesheet(ImmutableMap.of(XmlName.localName("paper.type"), "A4"))
              .transform(docBook);
      assertTrue(fo.contains("page-height=\"297mm\""));
      assertTrue(fo.contains("page-width=\"210mm\""));
      assertTrue(fo.contains("<fo:block"));
      assertTrue(fo.contains("On the Possibility of Going Home"));
    }
  }

  /**
   * <p>
   * If there is no “en.hyp” file, a warning complains that “org.apache.fop.hyphenation.Hyphenator -
   * I/O problem while trying to load en.hyp java.io.FileNotFoundException: projectroot/en.hyp”, but
   * then there is a single “Hyphenation possible? true”, and the process succeeds.
   * </p>
   * <p>
   * If the “en.hyp” file is present, org.apache.fop.hyphenation.Hyphenator issues no warning, then
   * there is a single “Hyphenation possible? true”, and the process succeeds.
   * </p>
   * <p>
   * Same behavior with Saxon than with Xalan.
   * </p>
   */
  @Test
  void testDocBookSimpleArticleToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article.xml").toString());
    DocBookConformityChecker.usingDefaults().verifyValid(docBook);

    final DocBookTransformer helper =
        DocBookTransformer.usingFactory(new org.apache.xalan.processor.TransformerFactoryImpl());
    final StreamSource myStyle =
        new StreamSource(DocBookTransformer.class.getResource("mystyle.xsl").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      helper.usingFoStylesheet(myStyle, ImmutableMap.of())
          .asDocBookToPdfTransformer(Path.of("non-existent-" + Instant.now()).toUri())
          .toStream(docBook, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }

  @Test
  @Disabled("Produces a warning for overly long line")
  void testDocBookSimpleArticleWithImageToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article with image.xml").toString());
    DocBookConformityChecker.usingDefaults().verifyValid(docBook);

    final DocBookTransformer helper =
        DocBookTransformer.usingFactory(new org.apache.xalan.processor.TransformerFactoryImpl());
    final StreamSource myStyle =
        new StreamSource(DocBookTransformer.class.getResource("mystyle.xsl").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      helper.usingFoStylesheet(myStyle, ImmutableMap.of())
          .asDocBookToPdfTransformer(Path.of("non-existent-" + Instant.now()).toUri())
          .toStream(docBook, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }

  @Test
  void testDocBookMissingImageToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article wrong image.xml").toString());
    DocBookConformityChecker.usingDefaults().verifyValid(docBook);

    final DocBookTransformer helper =
        DocBookTransformer.usingFactory(new net.sf.saxon.TransformerFactoryImpl());
    final StreamSource myStyle =
        new StreamSource(DocBookTransformer.class.getResource("mystyle.xsl").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      final ToBytesTransformer transformer = helper.usingFoStylesheet(myStyle, ImmutableMap.of())
          .asDocBookToPdfTransformer(Path.of("non-existent-" + Instant.now()).toUri());
      final XmlException exc =
          assertThrows(XmlException.class, () -> transformer.toStream(docBook, pdfStream));
      assertEquals(FileNotFoundException.class, exc.getCause().getClass());
    }
  }

  /**
   * Attempting to convert "docbook howto shortened.xml" to PDF fails. This seems to be too complex
   * for this process. Tables are not supported; and even without tables, it complains about some
   * line overflow (including without my custom styling). I didn’t investigate further.
   * <p>
   * If there is no “en.hyp” file, a warning complains that “org.apache.fop.hyphenation.Hyphenator -
   * I/O problem while trying to load en.hyp java.io.FileNotFoundException: projectRoot/en.hyp”,
   * then multiple “Hyphenation possible? true”, then (much later) “Hyphenation possible? false”.
   * </p>
   * <p>
   * If the “en.hyp” file is present, org.apache.fop.hyphenation.Hyphenator issues no warning, the
   * rest does not change.
   * </p>
   */
  @Test
  void testDocBookHowToShortenedToPdfSaxon() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto shortened.xml").toString());
    DocBookConformityChecker.usingDefaults().verifyValid(docBook);

    final DocBookTransformer helper =
        DocBookTransformer.usingFactory(new net.sf.saxon.TransformerFactoryImpl());
    final StreamSource myStyle =
        new StreamSource(DocBookTransformer.class.getResource("mystyle.xsl").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      assertThrows(XmlException.class,
          () -> helper.usingFoStylesheet(myStyle, ImmutableMap.of())
              .asDocBookToPdfTransformer(Path.of("non-existent-" + Instant.now()).toUri())
              .toStream(docBook, pdfStream));
    }
  }

  /**
   * Same behavior as with Saxon, with and without en.hyp.
   */
  @Test
  void testDocBookHowToShortenedToPdfXalan() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto shortened.xml").toString());
    DocBookConformityChecker.usingDefaults().verifyValid(docBook);

    final DocBookTransformer helper =
        DocBookTransformer.usingFactory(new org.apache.xalan.processor.TransformerFactoryImpl());
    final StreamSource myStyle =
        new StreamSource(DocBookTransformer.class.getResource("mystyle.xsl").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      assertThrows(XmlException.class,
          () -> helper.usingFoStylesheet(myStyle, ImmutableMap.of())
              .asDocBookToPdfTransformer(Path.of("non-existent-" + Instant.now()).toUri())
              .toStream(docBook, pdfStream));
    }
  }

  @Test
  void testDocBookSimpleArticleToXhtmlSaxon() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article.xml").toString());
    final XmlException xmlExc = assertThrows(XmlException.class,
        () -> DocBookTransformer.usingFactory(new net.sf.saxon.TransformerFactoryImpl())
            .usingXhtmlStylesheet(ImmutableMap.of()).transform(docBook));
    final String reason = xmlExc.getCause().getMessage();
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>Can't make chunks with Saxonica's processor.",
        reason);
  }

  @Test
  void testDocBookSimpleArticleToXhtmlXalan() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article.xml").toString());
    /*
     * new StreamSource(
     * "file:///usr/share/xml/docbook/stylesheet/docbook-xsl-ns/xhtml5/docbook.xsl")
     */
    final String xhtml =
        DocBookTransformer.usingFactory(new org.apache.xalan.processor.TransformerFactoryImpl())
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

  @Test
  void testDocBookSimpleArticleToXhtmlDefaultFactoryThrows() throws Exception {
    final DocBookTransformer transformer = DocBookTransformer.usingDefaultFactory();
    assertThrows(XmlException.class, () -> transformer.usingXhtmlStylesheet(ImmutableMap.of()));
  }

  @Test
  void testDocBookSimpleArticleToXhtmlSaxonParameterized() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article.xml").toString());
    /*
     * new StreamSource(
     * "file:///usr/share/xml/docbook/stylesheet/docbook-xsl-ns/xhtml5/docbook.xsl")
     */
    final String xhtml = DocBookTransformer.usingFactory(new net.sf.saxon.TransformerFactoryImpl())
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
}
