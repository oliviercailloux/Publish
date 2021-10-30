package io.github.oliviercailloux.publish;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.hyphenation.Hyphenator;
import org.apache.xmlgraphics.io.Resource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FopTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FopTests.class);

  @Test
  void testDirectAccess() throws Exception {
    /*
     * This works here because the access goes through the file system (thereby obtaining a file
     * URL), but it is not officially supported, as the resource "." may be considered to not exist
     * when it is not a regular folder. Indeed, this fails when the resources are put into a jar
     * file.
     */
    final URI hyphenatorUri = Hyphenator.class.getResource(".").toURI();
    LOGGER.info("Obtained direct URI: {}.", hyphenatorUri);

    final URL configUrl = DocBookHelper.class.getResource("fop-config.xml");
    try (InputStream configStream = configUrl.openStream()) {
      final FopFactory factory = FopFactory.newInstance(hyphenatorUri, configStream);
      try (Resource fr = factory.getHyphenationResourceResolver().getResource("fr.hyp")) {
        LOGGER.info("First byte read: {}.", fr.read());
      }
    }
  }

  @Test
  void testIndirectAccess() throws Exception {
    final URI hyphenatorFileUri = Hyphenator.class.getResource("en.hyp").toURI();
    LOGGER.info("Obtained file URI: {}.", hyphenatorFileUri);
    final URI hyphenatorUri = hyphenatorFileUri.resolve("");
    LOGGER.info("Obtained indirect URI: {}.", hyphenatorUri);

    final URL configUrl = DocBookHelper.class.getResource("fop-config.xml");
    try (InputStream configStream = configUrl.openStream()) {
      final FopFactory factory = FopFactory.newInstance(hyphenatorUri, configStream);
      try (Resource fr = factory.getHyphenationResourceResolver().getResource("fr.hyp")) {
        LOGGER.info("First byte read: {}.", fr.read());
      }
    }
  }
}
