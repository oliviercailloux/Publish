package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.VerifyException;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;

public class DocBookConformityCheckerTests {

  @Test
  void testValidArticle() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Simple article.dbk").toString());
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testValidHowto() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("Howto shortened.dbk").toString());
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testInvalidHowto() throws Exception {
    final StreamSource docBook =
        new StreamSource(DocBookTransformerTests.class.getResource("Howto invalid.dbk").toString());
    assertThrows(VerifyException.class,
        () -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }
}
