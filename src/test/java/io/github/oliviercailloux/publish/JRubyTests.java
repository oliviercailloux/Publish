package io.github.oliviercailloux.publish;

import com.google.common.collect.ImmutableList;
import org.asciidoctor.jruby.AsciidoctorJRuby;
import org.jruby.Ruby;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JRubyTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JRubyTests.class);

  @Test
  void testJRuby() throws Exception {
    Ruby.clearGlobalRuntime();
    LOGGER.info("Starting ruby.");
    System.setProperty("jruby.logger.class", EmptyLogger.class.getCanonicalName());
    Ruby runtime = Ruby.newInstance();
    LOGGER.info("Started ruby.");

    System.out.println(runtime.getLoadService().getLoadPath());
    final AsciidoctorJRuby a =
        AsciidoctorJRuby.Factory.create(ImmutableList.of("nonexisting"), "nonexisting");
  }
}
