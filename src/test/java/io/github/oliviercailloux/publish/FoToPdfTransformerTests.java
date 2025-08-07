package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlToBytesTransformer;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import javax.xml.transform.TransformerException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.RendererFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FoToPdfTransformerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FoToPdfTransformerTests.class);

  @ParameterizedTest
  @EnumSource
  void testHelloWorld(KnownFactory factoryFoToPdf) throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory())
        .bytesToBytes(Resourcer.byteSource("Hello world/Hello world A4.fo"));
    assertTrue(pdf.length >= 10);

    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      final PDFTextStripper stripper = new PDFTextStripper();
      final String text = stripper.getText(document);
      assertTrue(text.contains("Hello"));
    }

    ByteSource expected = Resourcer.byteSource("Hello world/Hello world A4.pdf");
    assertTrue(PdfCompar.compare(expected, ByteSource.wrap(pdf)).isEqual());
  }

  @ParameterizedTest
  @EnumSource
  void testHelloWorldA6(KnownFactory factoryFoToPdf) throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory())
        .bytesToBytes(Resourcer.byteSource("Hello world/Hello world A6.fo"));
    assertTrue(pdf.length >= 10);

    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      final PDFTextStripper stripper = new PDFTextStripper();
      final String text = stripper.getText(document);
      assertTrue(text.contains("Hello"));
    }

    assertTrue(PdfCompar
        .compare(Resourcer.byteSource("Hello world/Hello world A6.pdf"), ByteSource.wrap(pdf))
        .isEqual());
    assertFalse(PdfCompar
        .compare(Resourcer.byteSource("Hello world/Hello world A4.pdf"), ByteSource.wrap(pdf))
        .isEqual());
  }

  @ParameterizedTest
  @EnumSource
  void testConfigVerkeerdLogError(KnownFactory factoryFoToPdf) throws Exception {
    ByteSource config = Resourcer.byteSource("Support from Fo/fop-config TheVerkeerdFont.xml");
    XmlToBytesTransformer t = FoToPdfTransformer.withConfig(factoryFoToPdf.factory(), config);
    /*
     * The call new FOTreeBuilder(MimeConstants.MIME_PDF, foUserAgent, stream); triggers the
     * rendererFactory.createFOEventHandler. See #testConfigVerkeerdDirect.
     */
    LOGGER.info("Converting.");
    byte[] pdf = t.bytesToBytes(Resourcer.byteSource("Hello world/Hello world A4.fo"));
    LOGGER.info("Converted.");
    assertTrue(pdf.length >= 10);
  }

  /* This logs an error which I apparently cannot access. */
  @Test
  void testConfigVerkeerdDirect() throws Exception {
    final URI base = Path.of("").toUri();
    ByteSource config = Resourcer.byteSource("Support from Fo/fop-config TheVerkeerdFont.xml");
    FopFactory fopFactory;
    try (InputStream configStream = config.openBufferedStream();
        ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      fopFactory = FopFactory.newInstance(base, configStream);
      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      RendererFactory rendererFactory = foUserAgent.getRendererFactory();
      LOGGER.info("Creating event handler.");
      /*
       * This logs an error about font substitution. The corresponding code is in FontSubstitutions.
       * See
       * https://github.com/apache/xmlgraphics-fop/blob/main/fop-core/src/main/java/org/apache/fop/
       * fonts/substitute/FontSubstitutions.java. It seems that it cannot be intercepted. The
       * closest I get is to use foUserAgent.getFontManager().setFontSubstitutions, but there is no
       * access to the current one to delegate to it: FontManager#getFontSubstitutions is protected.
       */
      rendererFactory.createFOEventHandler(foUserAgent, MimeConstants.MIME_PDF, stream);
      LOGGER.info("Created event handler.");
    }
  }

  @ParameterizedTest
  @EnumSource
  void testConfigIncorrectIsSilent(KnownFactory factoryFoToPdf) throws Exception {
    ByteSource config = Resourcer.byteSource("Support from Fo/fop-config Incorrect.xml");
    assertDoesNotThrow(() -> FoToPdfTransformer.withConfig(factoryFoToPdf.factory(), config));
  }

  @ParameterizedTest
  @EnumSource
  void testConfigInvalid(KnownFactory factoryFoToPdf) throws Exception {
    ByteSource config = Resourcer.byteSource("Support from Fo/fop-config Invalid.xml");
    SAXException e = assertThrows(SAXException.class,
        () -> FoToPdfTransformer.withConfig(factoryFoToPdf.factory(), config));
    assertTrue(Throwables.getRootCause(e).getMessage().contains("DoesNotExist"));
  }

  @CartesianTest
  void testSimpleArticleRaw(
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).bytesToBytes(
        Resourcer.byteSource("Simple/Simple article.fo"));
    assertTrue(pdf.length >= 10);
    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      assertEquals(null, document.getDocumentInformation().getTitle());
    }
  }

  @CartesianTest
  void testSimpleArticleStyled(
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).bytesToBytes(
        Resourcer.byteSource("Simple/Simple article styled.fo"));
    assertTrue(pdf.length >= 10);
    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      assertEquals("My Article", document.getDocumentInformation().getTitle());
    }
  }

  @CartesianTest
  void testArticleWithImageThrows(@CartesianTest.Enum KnownFactory factoryFoToPdf)
      throws Exception {
    final XmlToBytesTransformer t = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory());
    final XmlException e = assertThrows(XmlException.class,
        () -> t.bytesToBytes(Resourcer.byteSource("With image/Article with image.fo")));
    final Throwable cause = e.getCause();
    assertEquals(TransformerException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("LineBreaking"));
  }

  @CartesianTest
  void testArticleWithSmallImage(
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).bytesToBytes(
        Resourcer.byteSource("Article with small image styled.fo"));
    assertTrue(pdf.length >= 10);
    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      assertEquals("My Article", document.getDocumentInformation().getTitle());
    }
  }

  @CartesianTest
  void testArticleWithNonExistingImageThrows(
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final XmlToBytesTransformer t = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory());
    final XmlException e = assertThrows(XmlException.class, () -> t.bytesToBytes(Resourcer
        .byteSource("Article with non existing image styled.fo")));
    final Throwable cause = e.getCause();
    assertEquals(TransformerException.class, cause.getClass());
    assertEquals(FileNotFoundException.class, cause.getCause().getClass());
  }

  /**
   * Attempting to convert "Howto shortened" to PDF fails. This seems to be too complex for this
   * process. Tables are not supported; and even without tables, it complains about some line
   * overflow (including without my custom styling). I didn’t investigate further.
   */
  @CartesianTest
  void testHowtoThrows(
      
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final XmlToBytesTransformer t = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory());
    final XmlException e = assertThrows(XmlException.class, () -> t.bytesToBytes(
        Resourcer.byteSource("Howto shortened styled.fo")));
    final Throwable cause = e.getCause();
    assertEquals(TransformerException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("LineBreaking"));
  }

  @ParameterizedTest
  @EnumSource
  void testOverlyLongLineHyphenates(KnownFactory factoryFoToPdf) throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory())
        .bytesToBytes(Resourcer.byteSource("Overly long line.fo"));
    assertTrue(pdf.length >= 10);
    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      assertEquals("My overly long line", document.getDocumentInformation().getTitle());
      final PDFTextStripper stripper = new PDFTextStripper();
      final String text = stripper.getText(document);
      assertTrue(text.contains("incomprehensibil-\n" + "ities"));
    }
  }

  @Test
  void testArticleWithPdf() throws Exception {
    final byte[] pdf = FoToPdfTransformer.usingFactory(KnownFactory.XALAN.factory())
        .bytesToBytes(Resourcer.byteSource("Include PDF.fo"));
    assertTrue(pdf.length >= 10);
    try (PDDocument document = Loader.loadPDF(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      final PDFTextStripper stripper = new PDFTextStripper();
      final String text = stripper.getText(document);
      assertTrue(text.contains("Hello"));
    }
  }
}
