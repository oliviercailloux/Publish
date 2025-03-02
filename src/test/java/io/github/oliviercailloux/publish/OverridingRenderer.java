package io.github.oliviercailloux.publish;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.area.LineArea;
import org.apache.fop.area.OffDocumentItem;
import org.apache.fop.area.PageSequence;
import org.apache.fop.area.PageViewport;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.render.Graphics2DAdapter;
import org.apache.fop.render.ImageAdapter;
import org.apache.fop.render.Renderer;

public class OverridingRenderer implements Renderer {
  private final Renderer delegate;

  public OverridingRenderer(Renderer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setupFontInfo(FontInfo fontInfo) throws FOPException {
    delegate.setupFontInfo(fontInfo);
  }

  @Override
  public void startRenderer(OutputStream o) throws IOException {
    delegate.startRenderer(o);
  }

  @Override
  public void stopRenderer() throws IOException {
    delegate.stopRenderer();
  }
  @Override
  public String getMimeType() {
    return delegate.getMimeType();
  }

  @Override
  public FOUserAgent getUserAgent() {
    return delegate.getUserAgent();
  }

  @Override
  public boolean supportsOutOfOrder() {
    return delegate.supportsOutOfOrder();
  }

  @Override
  public void setDocumentLocale(Locale locale) {
    delegate.setDocumentLocale(locale);
  }

  @Override
  public void processOffDocumentItem(OffDocumentItem odi) {
    delegate.processOffDocumentItem(odi);
  }

  @Override
  public Graphics2DAdapter getGraphics2DAdapter() {
    return delegate.getGraphics2DAdapter();
  }

  @Override
  public ImageAdapter getImageAdapter() {
    return delegate.getImageAdapter();
  }

  @Override
  public void preparePage(PageViewport page) {
    delegate.preparePage(page);
  }

  @Override
  @Deprecated
  public void startPageSequence(LineArea seqTitle) {
    delegate.startPageSequence(seqTitle);
  }

  @Override
  public void startPageSequence(PageSequence pageSequence) {
    delegate.startPageSequence(pageSequence);
  }

  @Override
  public void renderPage(PageViewport page) throws IOException, FOPException {
    delegate.renderPage(page);
  }

  
}
