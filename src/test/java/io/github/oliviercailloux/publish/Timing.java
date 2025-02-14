package io.github.oliviercailloux.publish;

import static io.github.oliviercailloux.publish.Resourcer.charSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Timing {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Timing.class);

  private static final Path L3_DIR = Path.of("../../java-course/L3/");

  public static void main(String[] args) throws Exception {
    // final URL idd = Timing.class.getResource("/org/apache/fop/accessibility/event-model.xml");
    final URI uri = Timing.class.getResource("/org/apache/fop/accessibility/").toURI();
    LOGGER.info("Event model URI: {}.", uri);
    LOGGER.info("Event model resolved URI: {}.", uri.resolve("event-model.xml"));
    // final URL url = Timing.class.getResource("/org/apache/fop/accessibility/");
    // LOGGER.info("Event model URL: {}.", url);
    // LOGGER.info("Event model parent URL: {}.", new URL(url, ".."));
    // final StreamSource streamSource = new StreamSource(uri.toString());
    // final String out = XmlTransformer.pedanticTransformer(KnownFactory.SAXON.factory())
    // .usingEmptyStylesheet().transform(streamSource);
    // LOGGER.info("Out: {}.", out);
    proceed();
  }

  static void proceed() throws IOException, MalformedURLException {
    final String adoc = Files.readString(L3_DIR.resolve("Lecture notes.adoc"));

    final String docBook;
    /* FIXME crash if file not found. */
    try (Asciidoctor adocConverter = Asciidoctor.Factory.create()) {
      LOGGER.info("Converting to Docbook.");
      // .sourceDir(L3_DIR.toFile())
      docBook = adocConverter.convert(adoc, Options.builder().standalone(true).backend("docbook")
          .baseDir(L3_DIR.toAbsolutePath().toFile()).safe(SafeMode.UNSAFE).build());
    }
    final CharSource docBookSource = CharSource.wrap(docBook);

    LOGGER.info("Validating Docbook.");
    DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBookSource);
    final TransformerFactory factory = new net.sf.saxon.TransformerFactoryImpl();
    final StreamSource myStyle = new StreamSource(
        Path.of("/home/olivier/Logiciels/fop/mystyle.xsl").toUri().toURL().toString());
    final XmlTransformer toFo =
        // DocBookTransformer.usingFactory(factory).usingStylesheet(myStyle, ImmutableMap.of());
        DocBookTransformer.usingFactory(factory).usingFoStylesheet(ImmutableMap.of());
    final String fo = toFo.charsToChars(docBookSource);
    final StreamSource foSource = new StreamSource(new StringReader(fo));
    final ToBytesTransformer toPdf = FoToPdfTransformer.usingFactory(factory, L3_DIR.toUri());
    toPdf.toSink(foSource, MoreFiles.asByteSink(L3_DIR.resolve("Lecture notes.pdf")));
  }

  @Test
  void testSimpleArticleToLocalXhtmlXalan() throws Exception {
    final CharSource docBook = charSource("Simple article.dbk");

    final CharSource localStyle = charSource(
        Path.of("file:///usr/share/xml/docbook/stylesheet/docbook-xsl-ns/xhtml5/docbook.xsl"));

    final String xhtml = XmlTransformerFactory.usingFactory(KnownFactory.XALAN.factory())
        .usingStylesheet(localStyle).charsToChars(docBook);
    LOGGER.debug("Resulting XHTML: {}.", xhtml);
    assertTrue(xhtml.contains("docbook.css"));
    final Element documentElement = DomHelper.domHelper()
        .asDocument(new StreamSource(new StringReader(xhtml))).getDocumentElement();
    final ImmutableList<Element> titleElements = DomHelper.toElements(
        documentElement.getElementsByTagNameNS(DomHelper.HTML_NS_URI.toString(), "title"));
    final Element titleElement = titleElements.stream().collect(MoreCollectors.onlyElement());
    assertEquals("My Article", titleElement.getTextContent());
  }
}
