package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Throwables;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.xml.KnownFactory;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.testutils.OutputCapturer;
import javax.xml.transform.TransformerFactory;
import org.junit.jupiter.api.Test;

public class DocBookWithJdkThrowsTests {

  @Test
  void testJdkFactoryFoThrows() throws Exception {
    final OutputCapturer capturer = OutputCapturer.capturer();
    capturer.capture();
    TransformerFactory underlying = KnownFactory.JDK.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory t = XmlTransformerFactory.usingFactory(underlying);
    XmlException e =
        assertThrows(XmlException.class, () -> t.usingStylesheet(DocBookResources.XSLT_1_FO_URI));
    String rootCause = Throwables.getRootCause(e).getMessage();
    assertTrue(rootCause.contains(
        "the compiler encountered XPath expressions with an accumulated '10 001' operators that exceeds the '10 000' limit set by 'FEATURE_SECURE_PROCESSING'."),
        "Root cause: " + rootCause);
    capturer.restore();
    assertTrue(capturer.out().isEmpty());
    assertTrue(capturer.err().lines().count() > 100);
  }

  @Test
  void testJdkFactoryHtmlThrows() throws Exception {
    final OutputCapturer capturer = OutputCapturer.capturer();
    capturer.capture();
    TransformerFactory underlying = KnownFactory.JDK.factory();
    underlying.setURIResolver(DocBookResources.RESOLVER);
    final XmlTransformerFactory t = XmlTransformerFactory.usingFactory(underlying);
    XmlException e = assertThrows(XmlException.class, () -> t.usingStylesheet(DocBookResources.XSLT_1_HTML_URI));
    String rootCause = Throwables.getRootCause(e).getMessage();
    assertTrue(rootCause.contains(
        "the compiler encountered XPath expressions with an accumulated '10 001' operators that exceeds the '10 000' limit set by 'FEATURE_SECURE_PROCESSING'."),
        "Root cause: " + rootCause);
    capturer.restore();
    assertTrue(capturer.out().isEmpty());
    assertTrue(capturer.err().lines().count() > 100);
  }
}
