package io.github.oliviercailloux.publish;

import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory.OutputProperties;
import java.net.URI;
import java.util.Map;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

public interface DocBookStylesheets {
  public interface Xslt1 {
    public static final URI TO_FO =
        URI.create("https://cdn.docbook.org/release/xsl/current/").resolve("fo/")
            .resolve("docbook.xsl");
    public static final URI TO_HTML =
        URI.create("https://cdn.docbook.org/release/xsl/current/").resolve("html/")
            .resolve("docbook.xsl");
    public static final URI TO_XHTML =
        URI.create("https://cdn.docbook.org/release/xsl/current/").resolve("xhtml5/")
            .resolve("docbook.xsl");
  }
}
