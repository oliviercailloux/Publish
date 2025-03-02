package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.ByteSource;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.fo.FOTreeBuilder;
import org.apache.fop.fonts.substitute.FontSubstitutions;
import org.apache.fop.render.RendererFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

public class FoToPdfTransformerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FoToPdfTransformerTests.class);

  @Test
  void testConfigVerkeerdManual() throws Exception {
    final URI base = PathUtils.fromResource(FoToPdfTransformerTests.class, ".").path().toUri();
    // TODO should crash but just logs the error, it does not go through the listener.
    ByteSource config =
        PathUtils.fromResource(FoToPdfTransformerTests.class, "fop-config TheVerkeerdFont.xml")
            .asByteSource();
    TransformerFactory factory = KnownFactory.XALAN.factory();
    URI fopBaseUri = base;
    FopFactory fopFactory;
    try (InputStream configStream = config.openBufferedStream()) {
      fopFactory = FopFactory.newInstance(fopBaseUri, configStream);
    }
    checkArgument(fopFactory.validateUserConfigStrictly());
    checkArgument(fopFactory.validateStrictly());
    final XmlTransformerFactory delegateTransformer =
        XmlTransformerFactory.pedanticTransformer(factory);

    final StreamSource source =
        new StreamSource(DocBookTransformerTests.class.getResource("Hello world A4.fo").toString());
    byte[] pdf;

    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      LOGGER.info("Creating user agent.");
      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      // foUserAgent.setFOEventHandlerOverride(new FOEventHandler() {

      // });
      LOGGER.info("Created user agent.");
      // foUserAgent.getEventBroadcaster().addEventListener(new LoggingEventListener());

      final EventListener l = e -> LOGGER.warn("Event: {}.", EventFormatter.format(e));
      LOGGER.info("Listening to FOP events.");
      foUserAgent.getEventBroadcaster().addEventListener(l);

      final Result res;
      try {
        LOGGER.info("Creating FOP.");
        // final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, destination);
        // res = new SAXResult(fop.getDefaultHandler());
        String outputFormat = MimeConstants.MIME_PDF;
        FontSubstitutions f = fopFactory.getFontManager().getFontSubstitutions();
        foUserAgent.getFontManager().setFontSubstitutions(new FontSubstitutions() {
          @Override
          public void adjustFontInfo(FontInfo fontInfo) {
            LOGGER.info("Adjusting font info.");
            f.adjustFontInfo(fontInfo);
          }
        });
        RendererFactory rendererFactory = foUserAgent.getRendererFactory();
        LOGGER.info("Getting.");
        rendererFactory.getFOEventHandlerMaker(outputFormat);
        LOGGER.info("Creating event handler.");
        rendererFactory.createFOEventHandler(foUserAgent, outputFormat, stream);
        // foUserAgent.getRendererFactory().cre
        //       this.renderer = userAgent.getRendererFactory().createRenderer(
          // userAgent, outputFormat);

          // try {
          //     renderer.setupFontInfo(fontInfo);
  
        LOGGER.info("Created event handler.");
        FOTreeBuilder builder = new FOTreeBuilder(outputFormat, foUserAgent, stream) {
          @Override
          public void warning(SAXParseException e) {
            LOGGER.warn(e.getLocalizedMessage());
          }

          @Override
          public void error(SAXParseException e) {
            LOGGER.warn(e.getLocalizedMessage());
          }

          @Override
          public void fatalError(SAXParseException e) {
            LOGGER.warn(e.getLocalizedMessage());
          }
        };
        LOGGER.info("Creating SAX result.");
        res = new SAXResult(builder);
        LOGGER.info("Created SAX result.");
      } catch (FOPException e) {
        throw new IllegalStateException(e);
      }

      LOGGER.info("Transforming.");
      delegateTransformer.usingEmptyStylesheet().sourceToResult(source, res);
      LOGGER.info("Transformed.");
      pdf = stream.toByteArray();
    }
    assertTrue(pdf.length >= 10);
    try (PDDocument document = PDDocument.load(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      final PDFTextStripper stripper = new PDFTextStripper();
      final String text = stripper.getText(document);
      assertTrue(text.contains("Hello"));
    }
  }

  @ParameterizedTest
  @EnumSource(names = {"XALAN"})
  void testConfigVerkeerd(KnownFactory factoryFoToPdf) throws Exception {
    final URI base = PathUtils.fromResource(FoToPdfTransformerTests.class, ".").path().toUri();
    // TODO should crash but just logs the error, it does not go through the listener.
    ByteSource configSource =
        PathUtils.fromResource(FoToPdfTransformerTests.class, "fop-config TheVerkeerdFont.xml")
            .asByteSource();
    ToBytesTransformer t =
        FoToPdfTransformer.usingFactory(factoryFoToPdf.factory(), base, configSource);
    final StreamSource src =
        new StreamSource(DocBookTransformerTests.class.getResource("Hello world A4.fo").toString());
    byte[] pdf = t.toBytes(src);
    assertTrue(pdf.length >= 10);
    try (PDDocument document = PDDocument.load(pdf)) {
      final int numberOfPages = document.getNumberOfPages();
      assertEquals(1, numberOfPages);
      final PDFTextStripper stripper = new PDFTextStripper();
      final String text = stripper.getText(document);
      assertTrue(text.contains("Hello"));
    }
  }

  @ParameterizedTest
  @EnumSource
  void testConfigIncorrect(KnownFactory factoryFoToPdf) throws Exception {
    final URI base = PathUtils.fromResource(FoToPdfTransformerTests.class, ".").path().toUri();
    ByteSource configSource = PathUtils
        .fromResource(FoToPdfTransformerTests.class, "fop-config Incorrect.xml").asByteSource();
    assertThrows(XmlException.class,
        () -> FoToPdfTransformer.usingFactory(factoryFoToPdf.factory(), base, configSource));
  }

  @CartesianTest
  void testSimpleArticleRaw(
      @CartesianTest.Enum(names = {"XALAN", "SAXON"}) KnownFactory factoryDocBookToFo,
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Simple article using %s raw.fo".formatted(factoryDocBookToFo)).toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      // Files.write(Path.of("using %s then %s.pdf".formatted(factoryDocBookToFo, factoryFoToPdf)),
      // pdf);
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals(null, document.getDocumentInformation().getTitle());
      }
    }
  }

  @Test
  void testSimpleArticleRawSS() throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Simple article using %s raw.fo".formatted(KnownFactory.SAXON)).toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(KnownFactory.SAXON.factory()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      // Files.write(Path.of("using %s then %s.pdf".formatted(factoryDocBookToFo, factoryFoToPdf)),
      // pdf);
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals(null, document.getDocumentInformation().getTitle());
      }
    }
  }

  @CartesianTest
  void testSimpleArticleStyled(
      @CartesianTest.Enum(names = {"XALAN", "SAXON"}) KnownFactory factoryDocBookToFo,
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Simple article using %s styled.fo".formatted(factoryDocBookToFo)).toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }

  @CartesianTest
  void testArticleWithImageThrows(
      @CartesianTest.Enum(names = {"XALAN", "SAXON"}) KnownFactory factoryDocBookToFo,
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Article with image using %s styled.fo".formatted(factoryDocBookToFo))
        .toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      final ToBytesTransformer t = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory());
      final XmlException e = assertThrows(XmlException.class, () -> t.toStream(src, pdfStream));
      final Throwable cause = e.getCause();
      assertEquals(TransformerException.class, cause.getClass());
      assertTrue(cause.getMessage().contains("LineBreaking"));
    }
  }

  @CartesianTest
  void testArticleWithSmallImage(
      @CartesianTest.Enum(names = {"XALAN", "SAXON"}) KnownFactory factoryDocBookToFo,
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Article with small image using %s styled.fo".formatted(factoryDocBookToFo))
        .toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }

  @CartesianTest
  void testArticleWithNonExistingImageThrows(
      @CartesianTest.Enum(names = {"XALAN", "SAXON"}) KnownFactory factoryDocBookToFo,
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource(
            "Article with non existing image using %s styled.fo".formatted(factoryDocBookToFo))
        .toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      final ToBytesTransformer t = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory());
      final XmlException e = assertThrows(XmlException.class, () -> t.toStream(src, pdfStream));
      final Throwable cause = e.getCause();
      assertEquals(TransformerException.class, cause.getClass());
      assertEquals(FileNotFoundException.class, cause.getCause().getClass());
    }
  }

  /**
   * Attempting to convert "Howto shortened" to PDF fails. This seems to be too complex for this
   * process. Tables are not supported; and even without tables, it complains about some line
   * overflow (including without my custom styling). I didn’t investigate further.
   */
  @CartesianTest
  void testHowtoThrows(
      @CartesianTest.Enum(names = {"XALAN", "SAXON"}) KnownFactory factoryDocBookToFo,
      @CartesianTest.Enum KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Howto shortened using %s styled.fo".formatted(factoryDocBookToFo))
        .toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      final ToBytesTransformer t = FoToPdfTransformer.usingFactory(factoryFoToPdf.factory());
      final XmlException e = assertThrows(XmlException.class, () -> t.toStream(src, pdfStream));
      final Throwable cause = e.getCause();
      assertEquals(TransformerException.class, cause.getClass());
      assertTrue(cause.getMessage().contains("LineBreaking"));
    }
  }

  @ParameterizedTest
  @EnumSource
  void testOverlyLongLineHyphenates(KnownFactory factoryFoToPdf) throws Exception {
    final StreamSource src = new StreamSource(
        DocBookTransformerTests.class.getResource("Overly long line.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(factoryFoToPdf.factory()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My overly long line", document.getDocumentInformation().getTitle());
        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);
        assertTrue(text.contains("incomprehensibil-\n" + "ities"));
      }
    }
  }

  @ParameterizedTest
  @EnumSource
  void testHelloWorld(KnownFactory factoryFoToPdf) throws Exception {
    URL input = DocBookTransformerTests.class.getResource("Hello world A4.fo");
    final StreamSource src = new StreamSource(input.toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      final URI base = PathUtils.fromResource(FoToPdfTransformerTests.class, ".").path().toUri();
      FoToPdfTransformer.usingFactory(factoryFoToPdf.factory(), base).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);
        assertTrue(text.contains("Hello"));
      }
    }
  }

  @Test
  void testArticleWithPdf() throws Exception {
    URL input = DocBookTransformerTests.class.getResource("Include PDF.fo");
    final StreamSource src = new StreamSource(input.toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      URI base = PathUtils.fromResource(FoToPdfTransformerTests.class, ".").path().toUri();
      FoToPdfTransformer.usingFactory(KnownFactory.XALAN.factory(), base).toStream(src, pdfStream);
      // TODO check log for font error.
      final byte[] pdf = pdfStream.toByteArray();
      assertTrue(pdf.length >= 10);
      Files.write(Path.of("Include PDF.pdf"), pdf);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);
        assertTrue(text.contains("Hello"));
      }
    }
  }
}
