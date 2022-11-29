package io.github.oliviercailloux.publish;

import com.google.common.io.ByteSink;
import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.Source;

public interface ToBytesTransformer {
  public void toStream(Source source, OutputStream destination) throws IOException, XmlException;

  public default void toSink(Source source, ByteSink destination)
      throws IOException, XmlException {
    try (OutputStream destStream = destination.openStream()) {
      toStream(source, destStream);
    }
  }
}