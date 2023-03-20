package io.github.oliviercailloux.testutils;

import static com.google.common.base.Preconditions.checkState;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class OutputCapturer {
  public static OutputCapturer capturer() {
    return new OutputCapturer();
  }

  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream outStream;
  private ByteArrayOutputStream errStream;

  private OutputCapturer() {
    originalOut = null;
    originalErr = null;
    outStream = null;
    errStream = null;
  }

  public void capture() {
    checkState(originalOut == null);
    originalOut = System.out;
    originalErr = System.err;
    outStream = new ByteArrayOutputStream();
    errStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStream));
    System.setErr(new PrintStream(errStream));
  }

  public void restore() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  public String out() {
    return outStream.toString();
  }

  public String err() {
    return errStream.toString();
  }
}
