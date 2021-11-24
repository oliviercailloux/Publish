package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.VerifyException;
import io.github.oliviercailloux.jaris.xml.XmlUtils.XmlException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import javax.xml.transform.stream.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DocBookHelperTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookHelperTests.class);

  @Test
  void testDocBookValid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto shortened.xml").toString());
    assertDoesNotThrow(() -> DocBookHelper.instance().verifyValid(docBook));
  }

  @Test
  void testDocBookInvalid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto invalid.xml").toString());
    assertThrows(VerifyException.class, () -> DocBookHelper.instance().verifyValid(docBook));
  }

  @Test
  void testFoToPdf() throws Exception {
    final StreamSource src =
        new StreamSource(DocBookHelperTests.class.getResource("article.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      DocBookHelper.instance().foToPdf(Path.of("non-existent-" + Instant.now()).toUri(), src,
          pdfStream);
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
  void testFoInvalidToPdf() throws Exception {
    final StreamSource src =
        new StreamSource(DocBookHelperTests.class.getResource("wrong.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      assertThrows(XmlException.class, () -> DocBookHelper.instance()
          .foToPdf(Path.of("non-existent-" + Instant.now()).toUri(), src, pdfStream));
    }
  }

  /**
   * Attempting to convert "docbook howto shortened.xml" to PDF fails. This seems to be too complex
   * for this process. Tables are not supported; and even without tables, it complains about some
   * line overflow (including without my custom styling). I didn’t investigate further.
   */
  @Test
  void testDocBookSimpleArticleToPdf() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook simple article.xml").toString());

    final DocBookHelper helper = DocBookHelper.instance();
    helper.verifyValid(docBook);
    final StreamSource myStyle =
        new StreamSource(DocBookHelper.class.getResource("mystyle.xsl").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      helper.docBookToPdf(Path.of("non-existent-" + Instant.now()).toUri(), docBook, myStyle,
          pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }
}
