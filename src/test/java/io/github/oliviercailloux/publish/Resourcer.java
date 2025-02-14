package io.github.oliviercailloux.publish;

import com.google.common.io.CharSource;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.io.PathUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Resourcer {
  public static CharSource charSource(String resourceName) {
    return PathUtils.fromResource(Resourcer.class, resourceName).asByteSource()
        .asCharSource(StandardCharsets.UTF_8);
  }

  public static CharSource charSource(URL source) {
    return Resources.asCharSource(source, StandardCharsets.UTF_8);
  }

  public static CharSource charSource(Path path) {
    return MoreFiles.asCharSource(path, StandardCharsets.UTF_8);
  }
}
