package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.xml.transform.stream.StreamSource;

@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
public class Resourcer {
  public static URL url(String resourceName) {
    return Resources.getResource(Resourcer.class, resourceName);
  }

  public static ByteSource byteSource(String resourceName) {
    return Resources.asByteSource(url(resourceName));
  }

  public static CharSource charSource(String resourceName) {
    return byteSource(resourceName).asCharSource(StandardCharsets.UTF_8);
  }

  public static StreamSource streamSource(String resourceName) {
    return new StreamSource(url(resourceName).toString());
  }

  public static CharSource charSource(URL source) {
    return Resources.asCharSource(source, StandardCharsets.UTF_8);
  }

  public static CharSource charSource(Path path) {
    return MoreFiles.asCharSource(path, StandardCharsets.UTF_8);
  }
}
