package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.VerifyException;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class DocBookConformityCheckerTests {

  @Test
  void testValidArticle() throws Exception {
    final URI docBook = DocBookToFoTests.class.getResource("Simple article.dbk").toURI();
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testValidHowto() throws Exception {
    final URI docBook = DocBookToFoTests.class.getResource("Howto shortened.dbk").toURI();
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testInvalidHowto() throws Exception {
    final URI docBook = DocBookToFoTests.class.getResource("Howto invalid.dbk").toURI();
    assertThrows(VerifyException.class,
        () -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }
}
