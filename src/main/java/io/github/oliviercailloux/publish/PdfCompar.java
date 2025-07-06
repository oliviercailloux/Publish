package io.github.oliviercailloux.publish;

import com.google.common.io.ByteSource;
import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.CompareResultImpl;
import de.redsix.pdfcompare.PdfComparator;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class PdfCompar {

  public static CompareResult compare(ByteSource expected, ByteSource actual) throws IOException {
    try (ByteArrayInputStream expectedStream = new ByteArrayInputStream(expected.read());
        ByteArrayInputStream actualStream = new ByteArrayInputStream(actual.read())) {
      PdfComparator<CompareResultImpl> p =
          new PdfComparator<CompareResultImpl>(expectedStream, actualStream);
      return p.compare();
    }
  }
}
