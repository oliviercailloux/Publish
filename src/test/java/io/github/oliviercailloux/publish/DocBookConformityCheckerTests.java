package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.VerifyException;
import com.google.common.io.CharSource;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class DocBookConformityCheckerTests {

  @Test
  void testValidArticle() throws Exception {
    final CharSource docBook = Resourcer.charSource("Simple/Simple article.dbk");
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testValidHowto() throws Exception {
    final CharSource docBook = Resourcer.charSource("Howto/Howto shortened.dbk");
    assertDoesNotThrow(() -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }

  @Test
  void testInvalidHowto() throws Exception {
    final CharSource docBook = Resourcer.charSource("Howto/Howto invalid.dbk");
    assertThrows(VerifyException.class,
        () -> DocBookConformityChecker.usingEmbeddedSchema().verifyValid(docBook));
  }
}
