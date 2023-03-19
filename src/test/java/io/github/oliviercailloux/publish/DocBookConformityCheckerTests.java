package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.base.VerifyException;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;

public class DocBookConformityCheckerTests {

  @Test
  void testDocBookValid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook howto shortened.xml").toString());
    assertDoesNotThrow(() -> DocBookConformityChecker.usingDefaults().verifyValid(docBook));
  }

  @Test
  void testDocBookValidArticle() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook simple article.xml").toString());
    assertDoesNotThrow(() -> DocBookConformityChecker.usingDefaults().verifyValid(docBook));
  }

  @Test
  void testDocBookInvalid() throws Exception {
    final StreamSource docBook = new StreamSource(
        DocBookTransformerTests.class.getResource("docbook howto invalid.xml").toString());
    assertThrows(VerifyException.class,
        () -> DocBookConformityChecker.usingDefaults().verifyValid(docBook));
  }
}
