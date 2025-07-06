package io.github.oliviercailloux.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PageArea;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PdfComparTests {

  @Test
  void testCompareText() throws Exception {
    ByteSource warld = Resourcer.byteSource("Hello world/Hello warld A4.pdf");
    ByteSource world = Resourcer.byteSource("Hello world/Hello world A4.pdf");
    CompareResult compared = PdfCompar.compare(warld, world);
    Collection<PageArea> differences = compared.getDifferences();
    assertEquals(1, differences.size());
    PageArea difference = Iterables.getOnlyElement(differences);
    assertEquals(1, difference.getPage());
    Map<Integer, Double> pageDiffsInPercent = compared.getPageDiffsInPercent();
    double diffpc = pageDiffsInPercent.get(Iterables.getOnlyElement(pageDiffsInPercent.keySet()));
    assertTrue(0d < diffpc);
    assertTrue(diffpc < 0.1d);
  }

  @Test
  void testCompareArea() throws Exception {
    ByteSource first = Resourcer.byteSource("Hello world/Hello world A4.pdf");
    ByteSource second = Resourcer.byteSource("Hello world/Hello world A6.pdf");
    CompareResult compared = PdfCompar.compare(first, second);
    Collection<PageArea> differences = compared.getDifferences();
    assertEquals(1, differences.size());
    PageArea difference = Iterables.getOnlyElement(differences);
    assertEquals(1, difference.getPage());
    Map<Integer, Double> pageDiffsInPercent = compared.getPageDiffsInPercent();
    double diffpc = pageDiffsInPercent.get(Iterables.getOnlyElement(pageDiffsInPercent.keySet()));
    assertTrue(60d < diffpc);
    assertTrue(diffpc < 80d, "" + diffpc);
  }
}
