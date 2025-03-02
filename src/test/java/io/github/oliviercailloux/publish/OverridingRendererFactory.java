package io.github.oliviercailloux.publish;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererFactory;

public class OverridingRendererFactory extends RendererFactory {
  
  private final RendererFactory delegate;
  
  public OverridingRendererFactory(boolean rendererPreferred, RendererFactory delegate) {
      super(rendererPreferred);
      this.delegate = delegate;
    }
  @Override
  public Renderer createRenderer(FOUserAgent userAgent, String outputFormat)
                    throws FOPException {
    return delegate.createRenderer(userAgent, outputFormat);
  }
  
}
