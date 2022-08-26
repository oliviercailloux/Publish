package io.github.oliviercailloux.publish;

import java.io.PrintStream;
import org.jruby.util.log.Logger;

public class EmptyLogger implements Logger {

	public EmptyLogger(@SuppressWarnings("unused") String loggerName) {
	}

	public EmptyLogger(@SuppressWarnings("unused") Class<?> loggerClass) {
	}

	public EmptyLogger(@SuppressWarnings("unused") String loggerName, @SuppressWarnings("unused") PrintStream stream) {
	}

	@Override
	public String getName() {
		return "name";
	}

	@Override
	public void warn(String message, Object... args) {

	}

	@Override
	public void warn(Throwable throwable) {

	}

	@Override
	public void warn(String message, Throwable throwable) {

	}

	@Override
	public void error(String message, Object... args) {

	}

	@Override
	public void error(Throwable throwable) {

	}

	@Override
	public void error(String message, Throwable throwable) {

	}

	@Override
	public void info(String message, Object... args) {

	}

	@Override
	public void info(Throwable throwable) {

	}

	@Override
	public void info(String message, Throwable throwable) {

	}

	@Override
	public void debug(String message, Object... args) {

	}

	@Override
	public void debug(Throwable throwable) {

	}

	@Override
	public void debug(String message, Throwable throwable) {

	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public void setDebugEnable(boolean debug) {

	}
}
