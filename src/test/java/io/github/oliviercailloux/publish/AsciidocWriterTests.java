package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.jruby.util.log.SLF4JLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class AsciidocWriterTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(AsciidocWriterTests.class);

  @BeforeAll
  static void setJrubyLogger() {
    System.setProperty("jruby.logger.class", SLF4JLogger.class.getCanonicalName());
  }

  private String getSingleParagraph(String xml) throws XmlException, IOException {
    final Document adocXmlDoc =
        DomHelper.domHelper().asDocument(new StreamSource(new StringReader(xml)));
    final Element root = adocXmlDoc.getDocumentElement();
    assertEquals("simpara", root.getTagName());
    final NodeList childNodes = root.getChildNodes();
    assertEquals(1, childNodes.getLength());
    final Node childNode = childNodes.item(0);
    assertEquals(Node.TEXT_NODE, childNode.getNodeType());
    final String textContent = childNode.getNodeValue();
    return textContent;
  }

  @Test
  void testComplex() throws Exception{
    final AsciidocWriter writer = new AsciidocWriter();
    String complex =
        "a *starred* version and an _underlined_, ‘quoted’, a http://url.com url ’’ two-quotes, with `back-quoted * star` and also `+` plus ``++` double-plus";
    writer.verbatim(complex);
    final String written = writer.getContent();

    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      final String adocXml =
          adocConverter.convert(written, Options.builder().backend("docbook").build());
      LOGGER.debug("Xml: {}.", adocXml);
      final String textContent = getSingleParagraph(adocXml);
      assertEquals(complex, textContent);
    }

    writer.verbatim("a *starred* on\ntwo *`lines`*!");
  }

  @Test
  void testTwoLines() throws Exception {
    final AsciidocWriter writer = new AsciidocWriter();
    final String content = "a *starred* on\ntwo *`lines`*!";
    writer.verbatim(content);
    final String written = writer.getContent();

    LOGGER.info("Creating.");
    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      LOGGER.info("Created.");
      final String adocXml =
          adocConverter.convert(written, Options.builder().backend("docbook").build());
      LOGGER.debug("Xml: {}.", adocXml);
      final String textContent = getSingleParagraph(adocXml);
      assertEquals(content, textContent);
    }
  }

  @Test
  void testValid() throws Exception {
    final AsciidocWriter writer = new AsciidocWriter();
    final String content = "a *starred* on\ntwo *`lines`*!";
    writer.verbatim(content);
    final String written = writer.getContent();

    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      {
        final String docBookPartial =
            adocConverter.convert(written, Options.builder().backend("docbook").build());
        assertThrows(VerifyException.class, () -> DocBookConformityChecker.usingEmbeddedSchema()
            .verifyValid(new StreamSource(new StringReader(docBookPartial))));
      }
      {
        final String docBookFull = adocConverter.convert(written,
            Options.builder().standalone(true).backend("docbook").build());
        assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema()
            .verifyValid(new StreamSource(new StringReader(docBookFull))));
      }
    }
  }

  @Test
  void testTransform() throws Exception {
    final AsciidocWriter writer = new AsciidocWriter();
    final String content = "a *starred* on\ntwo *`lines`*!";
    writer.verbatim(content);
    final String written = writer.getContent();

    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      final String docBookFull = adocConverter.convert(written,
          Options.builder().standalone(true).backend("docbook").build());
      final StreamSource docBookInput = new StreamSource(new StringReader(docBookFull));
      final String transformed =
          XmlTransformerFactory.usingFactory(new net.sf.saxon.TransformerFactoryImpl())
              .usingStylesheet(DocBookStylesheets.Xslt1.TO_FO).sourceToChars(docBookInput);
      assertTrue(transformed.contains("page-height=\"11in\""));
      assertTrue(transformed.contains("\ntwo *`lines`*!</fo:block>"));
    }
  }

  @Test
  void testTransformLink() throws Exception {
    URL inputUrl = AsciidocWriterTests.class.getResource("Various Asciidoc/Link.adoc");
    final String inputStr = Files.readString(Path.of(inputUrl.toURI()));
    LOGGER.info("Creating Asciidoctor converter.");
    final String docBook;
    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      LOGGER.info("Converting to Docbook.");
      docBook = adocConverter.convert(inputStr, Options.builder().standalone(true).backend("docbook")
          // .baseDir(L3_DIR.toAbsolutePath().toFile())
          .safe(SafeMode.UNSAFE).build());
    }
    LOGGER.info("Validating Docbook.");
    LOGGER.debug("Docbook: {}.", docBook);
    DocBookConformityChecker.usingEmbeddedSchema()
        .verifyValid(new StreamSource(new StringReader(docBook)));
  }

  @Test
  void testTransformPresent() throws Exception {
    URL inputUrl = AsciidocWriterTests.class.getResource("vrac/A Java course present.adoc");
    final String inputStr = Files.readString(Path.of(inputUrl.toURI()));
    LOGGER.info("Creating Asciidoctor converter.");
    final String docBook;
    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      LOGGER.info("Converting to Docbook.");
      docBook = adocConverter.convert(inputStr, Options.builder().standalone(true).backend("docbook")
          // .baseDir(L3_DIR.toAbsolutePath().toFile())
          .safe(SafeMode.UNSAFE).build());
    }
    LOGGER.info("Validating Docbook.");
    LOGGER.debug("Docbook: {}.", docBook);
    DocBookConformityChecker.usingEmbeddedSchema()
        .verifyValid(new StreamSource(new StringReader(docBook)));
  }

  @Test
  void testTable() throws Exception {
    final AsciidocWriter writer = new AsciidocWriter();
    final ImmutableList<String> r1 = ImmutableList.of("c1", "c2");
    final ImmutableList<String> r2 = ImmutableList.of();
    final ImmutableList<String> r3 = ImmutableList.of("c1 last");
    writer.table("1, 1", ImmutableList.of("h1", "h2"), ImmutableList.of(r1, r2, r3));

    final String expected =
        Files.readString(Path.of(AsciidocWriterTests.class.getResource("Various Asciidoc/Table.adoc").toURI()));
    assertEquals(expected, writer.getContent());
  }
}
