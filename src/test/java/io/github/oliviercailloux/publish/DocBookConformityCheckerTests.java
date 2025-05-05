package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.VerifyException;
import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.SchemaHelper;
import java.net.URI;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import org.junit.jupiter.api.Test;

public class DocBookConformityCheckerTests {

  @Test
  void testValidArticle() throws Exception {
    final URI docBook = DocBookTransformerTests.class.getResource("Simple article.dbk").toURI();
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testValidHowto() throws Exception {
    final URI docBook = DocBookTransformerTests.class.getResource("Howto shortened.dbk").toURI();
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testInvalidHowto() throws Exception {
    final URI docBook = DocBookTransformerTests.class.getResource("Howto invalid.dbk").toURI();
    assertThrows(VerifyException.class,
        () -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }
}
