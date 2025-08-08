package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Resources;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class UriTests {
  @Test
  public void testUri() {
    final URL configUrl = Resources.getResource(FoToPdfTransformer.class, "fop-config.xml");
    assertTrue(configUrl.toString().startsWith("file:/"));
    assertTrue(configUrl.toString()
        .endsWith("/target/classes/io/github/oliviercailloux/publish/fop-config.xml"));
    final URI base = Unchecker.URI_UNCHECKER.getUsing(() -> configUrl.toURI()).resolve("..");
    assertTrue(base.toString().startsWith("file:/"));
    assertTrue(base.toString().endsWith("/target/classes/io/github/oliviercailloux/"));
  }

  @Test
  public void testUriEmbedded() {
    assertNull(DocBookResources.class.getResource("."));
    final URL configUrlDir = Resources.getResource(DocBookResources.class, "..");
    assertTrue(configUrlDir.toString().startsWith("file:/"));
    assertTrue(configUrlDir.toString().endsWith("target/test-classes/io/github/oliviercailloux/"));

    final URL configUrl = Resources.getResource(DocBookResources.class, "fo/docbook.xsl");
    assertTrue(configUrl.toString().startsWith("jar:file:/"));
    assertTrue(
        configUrl.toString().endsWith("/io/github/oliviercailloux/docbook/0.0.4/docbook-0.0.4.jar!"
            + "/io/github/oliviercailloux/docbook/fo/docbook.xsl"),
        configUrl.toString());

    URI asUri = Unchecker.URI_UNCHECKER.getUsing(() -> configUrl.toURI());
    assertTrue(asUri.toString().startsWith("jar:file:/"));
    assertTrue(
        asUri.toString().endsWith("/io/github/oliviercailloux/docbook/0.0.4/docbook-0.0.4.jar!"
            + "/io/github/oliviercailloux/docbook/fo/docbook.xsl"));

    assertEquals(".", asUri.resolve(".").toString());
    assertEquals("..", asUri.resolve("..").toString());
  }

  @Test
  public void testCatalog() {
    String match = DocBookResources.CATALOG
        .matchSystem("http://cdn.docbook.org/release/xsl/1.79.2/anythingreally");
    assertTrue(match.toString().startsWith("jar:file:/"));
    assertTrue(
        match.toString().endsWith("/io/github/oliviercailloux/docbook/0.0.4/docbook-0.0.4.jar!"
            + "/io/github/oliviercailloux/docbook/anythingreally"));
  }
}
