package io.github.oliviercailloux.publish;

import org.jruby.Ruby;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JRubyTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JRubyTests.class);

  @Test
  void testJRuby() throws Exception {
    LOGGER.info("Starting ruby.");
    System.setProperty("jruby.logger.class", EmptyLogger.class.getCanonicalName());
    Ruby.newInstance();
    LOGGER.info("Started ruby.");
  }
}
