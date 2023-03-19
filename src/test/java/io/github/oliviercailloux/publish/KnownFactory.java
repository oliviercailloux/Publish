package io.github.oliviercailloux.publish;

import javax.xml.transform.TransformerFactory;

public enum KnownFactory {
  JDK(TransformerFactory.newDefaultInstance()),

  XALAN(new org.apache.xalan.processor.TransformerFactoryImpl()),

  SAXON(new net.sf.saxon.TransformerFactoryImpl());

  private final TransformerFactory factory;

  private KnownFactory(TransformerFactory t) {
    this.factory = t;
  }

  public TransformerFactory factory() {
    return factory;
  }
}
