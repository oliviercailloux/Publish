package io.github.oliviercailloux.publish;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

public class PdfTextStripperTests {
  @Test
  void testLoadFont() throws Exception {
    /* https://github.com/apache/pdfbox/blob/2.0/pdfbox/src/main/java/org/apache/pdfbox/pdmodel/font/PDType1Font.java: v2 loads all fonts as soon as it loads the class itself, thus, warns against missing font. */
    PDType1Font.COURIER.toString();
    /* https://github.com/apache/pdfbox/blob/3.0/pdfbox/src/main/java/org/apache/pdfbox/pdmodel/font/PDType1Font.java: v3 might not have the problem */
  }
}
