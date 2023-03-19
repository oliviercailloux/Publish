package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Instant;
import javax.xml.transform.stream.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DocBookToFoToPdfTransformerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER =
      LoggerFactory.getLogger(DocBookToFoToPdfTransformerTests.class);

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
   * <p>
   * ABout hyphenation, we can use, but not necessarily modify, en (en, en_GB and en_US), see
   * https://svn.lal.in2p3.fr/LCG/QWG/External/offo-hyphenation-fop-1.0/licenses.html#en and
   * https://opensource.stackexchange.com/q/10426; and FR, see
   * https://markmail.org/message/scd3ciflggk6e5rs
   * </p>
   */
  @Test
  void testSimpleArticleToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(DocBookToFoToPdfTransformerTests.class
        .getResource("docbook simple article.xml").toString());
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
  void testSimpleArticleWithImageToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(DocBookToFoToPdfTransformerTests.class
        .getResource("docbook simple article with image.xml").toString());
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
  void testMissingImageToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(DocBookToFoToPdfTransformerTests.class
        .getResource("docbook simple article wrong image.xml").toString());
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
  void testHowToShortenedToPdfSaxon() throws Exception {
    final StreamSource docBook = new StreamSource(DocBookToFoToPdfTransformerTests.class
        .getResource("docbook howto shortened.xml").toString());
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
  void testHowToShortenedToPdfXalan() throws Exception {
    final StreamSource docBook = new StreamSource(DocBookToFoToPdfTransformerTests.class
        .getResource("docbook howto shortened.xml").toString());
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
}
