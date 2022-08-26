package io.github.oliviercailloux.publish;

import com.google.common.collect.ImmutableMap;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.log.SLF4JLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See https://github.com/jruby/jruby/issues/6925
 */
public class JRubyTests {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(JRubyTests.class);

	@Test
	void testRedirectLoggingWorks() throws Exception {
		LOGGER.info("Starting ruby.");
		System.setProperty("jruby.logger.class", SLF4JLogger.class.getCanonicalName());
		Ruby.newInstance();
		LOGGER.info("Started ruby.");
	}

	@Test
	void testRedirectLoggingFails() throws Exception {
		LOGGER.info("Starting ruby.");
		final RubyInstanceConfig config = new RubyInstanceConfig();
		config.setEnvironment(ImmutableMap.of("jruby.logger.class", SLF4JLogger.class.getCanonicalName()));
		Ruby.newInstance(config);
		LOGGER.info("Started ruby.");
	}
}
