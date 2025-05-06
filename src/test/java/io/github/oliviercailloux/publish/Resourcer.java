package io.github.oliviercailloux.publish;

import com.google.common.base.VerifyException;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.io.CloseablePath;
import io.github.oliviercailloux.jaris.io.CloseablePathFactory;
import io.github.oliviercailloux.jaris.io.PathUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;

public class Resourcer {
  private static CloseablePathFactory factory(String resourceName) {
    return PathUtils.fromResource(Resourcer.class, resourceName);
  }

  public static CloseablePath path(String resourceName) {
    try {
      return factory(resourceName).path();
    } catch (ProviderNotFoundException | IOException e) {
      throw new VerifyException(e);
    }
  }  

  public static ByteSource byteSource(String resourceName) {
    return factory(resourceName).asByteSource();
  }  

  public static CharSource charSource(String resourceName) {
    return byteSource(resourceName).asCharSource(StandardCharsets.UTF_8);
  }

  public static CharSource charSource(URL source) {
    return Resources.asCharSource(source, StandardCharsets.UTF_8);
  }

  public static CharSource charSource(Path path) {
    return MoreFiles.asCharSource(path, StandardCharsets.UTF_8);
  }
}
