package io.github.oliviercailloux.publish;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteStreams;
import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

public interface ToBytesTransformer {
  /**
   * @param source
   * @param destination
   * @throws IOException
   * @throws XmlException wraps a {@link TransformerException}.
   */
  public void toStream(Source source, OutputStream destination) throws IOException, XmlException;

  /**
   * @param source
   * @param destination
   * @throws IOException
   * @throws XmlException wraps a {@link TransformerException}.
   */
  public default void toSink(Source source, ByteSink destination) throws IOException, XmlException {
    try (OutputStream destStream = destination.openStream()) {
      toStream(source, destStream);
    }
  }

  public default byte[] toBytes(Source source) throws IOException, XmlException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      toStream(source, out);
      return out.toByteArray();
    }
  }
}
