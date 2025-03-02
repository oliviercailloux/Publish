package io.github.oliviercailloux.publish;

import org.apache.fop.apps.io.InternalResourceResolver;
import org.apache.fop.fonts.FontCacheManager;
import org.apache.fop.fonts.FontDetector;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.FontManager;
import org.apache.fop.fonts.substitute.FontSubstitutions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyFontManager extends FontManager {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(MyFontManager.class);

  public MyFontManager(InternalResourceResolver resourceResolver, FontDetector fontDetector,
      FontCacheManager fontCacheManager) {
    super(resourceResolver, fontDetector, fontCacheManager);
  }

  @SuppressWarnings("serial")
  @Override
  public FontSubstitutions getFontSubstitutions() {
    FontSubstitutions delegate = super.getFontSubstitutions();
    return new FontSubstitutions() {
      @Override
      public void adjustFontInfo(FontInfo fontInfo) {
        LOGGER.info("Adjusting font info.");
        delegate.adjustFontInfo(fontInfo);
      }
    }
  }
}
