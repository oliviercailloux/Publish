package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

public class FoToPdfTransformerTests {

  @ParameterizedTest
  @MethodSource("biFactories")
  void testSimpleArticleToPdf(KnownFactory factoryDocBookToFo, KnownFactory factoryFoToPdf)
      throws Exception {
    final StreamSource src = new StreamSource(DocBookTransformerTests.class
        .getResource("Simple article using %s raw.fo".formatted("SAXON")).toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(knownFactory.factory())
          .usingBaseUri(Path.of("non-existent-" + Instant.now()).toUri()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      // Files.write(Path.of("out.pdf"), pdf);
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals(null, document.getDocumentInformation().getTitle());
      }
    }
  }

  private static Stream<Arguments> biFactories() {
    return Stream.of(Arguments.arguments());
  }

  @ParameterizedTest
  @EnumSource
  void testSimpleArticleUsingXalanToPdf(KnownFactory knownFactory) throws Exception {
    final StreamSource src = new StreamSource(
        DocBookTransformerTests.class.getResource("Simple article using XALAN.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(knownFactory.factory())
          .usingBaseUri(Path.of("non-existent-" + Instant.now()).toUri()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      // Files.write(Path.of("out.pdf"), pdf);
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }

  @ParameterizedTest
  @EnumSource
  void testArticleToPdf(KnownFactory knownFactory) throws Exception {
    final StreamSource src =
        new StreamSource(DocBookTransformerTests.class.getResource("article.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      FoToPdfTransformer.usingFactory(knownFactory.factory())
          .usingBaseUri(Path.of("non-existent-" + Instant.now()).toUri()).toStream(src, pdfStream);
      final byte[] pdf = pdfStream.toByteArray();
      // Files.write(Path.of("out.pdf"), pdf);
      assertTrue(pdf.length >= 10);
      try (PDDocument document = PDDocument.load(pdf)) {
        final int numberOfPages = document.getNumberOfPages();
        assertEquals(1, numberOfPages);
        assertEquals("My Article", document.getDocumentInformation().getTitle());
      }
    }
  }

  @ParameterizedTest
  @EnumSource
  void testFoInvalidToPdf(KnownFactory knownFactory) throws Exception {
    final StreamSource src =
        new StreamSource(DocBookTransformerTests.class.getResource("wrong.fo").toString());
    try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
      final ToBytesTransformer t = FoToPdfTransformer.usingFactory(knownFactory.factory())
          .usingBaseUri(Path.of("non-existent-" + Instant.now()).toUri());
      final XmlException e = assertThrows(XmlException.class, () -> t.toStream(src, pdfStream));
      final Throwable cause = e.getCause();
      assertEquals(TransformerException.class, cause.getClass());
      assertEquals(FileNotFoundException.class, cause.getCause().getClass());
    }
  }
}
