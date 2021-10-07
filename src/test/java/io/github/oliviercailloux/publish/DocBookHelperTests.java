package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.VerifyException;
import io.github.oliviercailloux.jaris.xml.XmlUtils.XmlException;
import java.io.ByteArrayOutputStream;
import javax.xml.transform.stream.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

class DocBookHelperTests {

  @Test
  void testValid() throws Exception {
    final StreamSource docBook =
        new StreamSource(DocBookHelperTests.class.getResource("docbook howto.xml").toString());
    assertDoesNotThrow(() -> DocBookHelper.instance().verifyValid(docBook));
  }

  @Test
  void testInvalid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookHelperTests.class.getResource("docbook howto invalid.xml").toString());
    assertThrows(VerifyException.class, () -> DocBookHelper.instance().verifyValid(docBook));
  }

  @Test
  void testPdf() throws Exception {
    final StreamSource src =
        new StreamSource(DocBookHelperTests.class.getResource("article.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      DocBookHelper.instance().foToPdf(src, pdfStream);
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
  void testPdfFailure() throws Exception {
    final StreamSource src =
        new StreamSource(DocBookHelperTests.class.getResource("wrong-fop.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      assertThrows(XmlException.class, () -> DocBookHelper.instance().foToPdf(src, pdfStream));
    }
  }
}
