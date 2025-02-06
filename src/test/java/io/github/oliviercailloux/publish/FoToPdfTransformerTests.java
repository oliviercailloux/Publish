package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class FoToPdfTransformerTests {

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
  @Test
  void testArticleWithPdfDetails() throws Exception {
    URL input = DocBookTransformerTests.class.getResource("Include PDF complex.fo");
    final StreamSource source = new StreamSource(input.toString());
    try (ByteArrayOutputStream destination = new ByteArrayOutputStream()) {
      URI fopBaseUri = PathUtils.fromResource(FoToPdfTransformerTests.class, ".").path().toUri();
      LOGGER.info("Base: {}.", fopBaseUri);
      TransformerFactory factory = KnownFactory.XALAN.factory();
      final URL configUrl = DocBookTransformer.class.getResource("fop-config.xml");
      final FopFactory fopFactory;
      try (InputStream configStream = configUrl.openStream()) {
        fopFactory = FopFactory.newInstance(fopBaseUri, configStream);
      }
      final XmlTransformer delegateTransformer = XmlTransformer.usingFactory(factory);

      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      // foUserAgent.getEventBroadcaster().addEventListener(new LoggingEventListener());

      final FoEventListener l = new FoEventListener();
      foUserAgent.getEventBroadcaster().addEventListener(l);

      final Result res;
      try {
        final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, destination);
        res = new SAXResult(fop.getDefaultHandler());
      } catch (FOPException e) {
        throw new IllegalStateException(e);
      }

      delegateTransformer.usingEmptyStylesheet().transform(source, res);
      /*
       * This duplicates the serious event that will get thrown in the log, but we’d better do that
       * so that one can see the order of events in the log and thus where the first serious one
       * happened exactly.
       */
      l.logAll();
      assertEquals(1, l.seriouses().size());
      Event serious = Iterables.getOnlyElement(l.seriouses());
      LOGGER.error("First serious event: {}.", EventFormatter.format(serious));
      Set<String> pKeys = serious.getParams().keySet();
      LOGGER.error("Keys: {}.", pKeys);
      String eventGroupID = serious.getEventGroupID();
      LOGGER.error("Event group ID: {}.", eventGroupID);
      String eventID = serious.getEventID();
      String key = Iterables.getOnlyElement(pKeys);
      RuntimeException value = (RuntimeException) serious.getParam(key);
      LOGGER.error("Value.", value);
      // l.seriouses().stream().findFirst().ifPresent(e -> {
      // throw FoEventListener.asException(e);
      // });
      final byte[] pdf = destination.toByteArray();
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
