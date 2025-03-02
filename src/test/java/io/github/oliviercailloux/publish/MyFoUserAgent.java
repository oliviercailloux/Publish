package io.github.oliviercailloux.publish;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.io.InternalResourceResolver;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.substitute.FontSubstitutions;

public class MyFoUserAgent extends FOUserAgent {

  MyFoUserAgent(FopFactory factory, InternalResourceResolver resourceResolver) {
    super(factory, resourceResolver);
  }

  @Override
  public FontInfo getFontInfo() {
    FontInfo delegate = super.getFontInfo();
    return new FontInfo() {
      @Override
      public void setFontSubstitutions(FontSubstitutions substitutions) {
        LOGGER.info("Setting font substitutions.");
        delegate.setFontSubstitutions(substitutions);
      }
    };
  }
  
}
