package io.github.oliviercailloux.publish;

import com.google.common.io.ByteSource;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.render.RendererFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoToPdfDirectTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FoToPdfDirectTests.class);

  @Test
  public void testFoToPdfDirect() throws Exception {
    final URI base = Path.of("").toUri();
    ByteSource config = Resourcer.byteSource("fop-config.xml");
    FopFactory fopFactory;
    try (InputStream configStream = config.openBufferedStream();
        ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      fopFactory = FopFactory.newInstance(base, configStream);
      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      RendererFactory rendererFactory = foUserAgent.getRendererFactory();
      LOGGER.info("Creating event handler.");
      rendererFactory.createFOEventHandler(foUserAgent, MimeConstants.MIME_PDF, stream);
      LOGGER.info("Created event handler.");
      OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("myfile.pdf")));

      try {
        // Step 3: Construct fop with desired output format
        Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

        // Step 4: Setup JAXP using identity transformer
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(); // identity transformer

        // Step 5: Setup input and output for XSLT transformation
        // Setup input stream
        Source src = new StreamSource(new File(
            "/home/olivier/Local/Nextcloud LAMSADE/Professions/Enseignement/Java & HTML/Publish/src/test/resources/io/github/oliviercailloux/publish/With image/Article with image.fo"));

        // Resulting SAX events (the generated FO) must be piped through to FOP
        Result res = new SAXResult(fop.getDefaultHandler());

        // Step 6: Start XSLT transformation and FOP processing
        transformer.transform(src, res);

      } finally {
        // Clean-up
        out.close();
      }
    }
  }
}
