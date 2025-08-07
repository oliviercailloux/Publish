package io.github.oliviercailloux.publish;

import static com.google.common.base.Verify.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.VerifyException;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlToBytesTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FoToPdfDirectTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FoToPdfDirectTests.class);

  @Test
  public void testFoToPdfDirect() throws Exception {
    final URI base = Path.of("").toUri();
    ByteSource config = Resourcer.byteSource("fop-config.xml");
    FopFactory fopFactory;
    try (InputStream configStream = config.openBufferedStream();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("out.pdf")))) {
      fopFactory = FopFactory.newInstance(base, configStream);

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
