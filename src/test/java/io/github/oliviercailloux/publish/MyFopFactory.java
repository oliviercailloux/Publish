package io.github.oliviercailloux.publish;

public class MyFopFactory extends FopFactory {
  public MyFopFactory(InternalResourceResolver resourceResolver) {
    super(resourceResolver);
  }

  @Override
  public FontManager createFontManager(FontDetector fontDetector, FontCacheManager fontCacheManager) {
    return new MyFontManager(getResourceResolver(), fontDetector, fontCacheManager);
  }
  
}
