package io.github.oliviercailloux.publish;

import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.SchemaHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocBookConformityChecker {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookConformityChecker.class);

  public static final SchemaFactory SCHEMA_FACTORY = new XMLSyntaxSchemaFactory();
  public static final StreamSource SCHEMA_SOURCE =
      new StreamSource(DocBookConformityChecker.class.getResource("docbook.rng").toString());

  public static ConformityChecker usingDefaults() throws XmlException {
    return cc(SCHEMA_FACTORY, SCHEMA_SOURCE);
  }

  static ConformityChecker cc(SchemaFactory schemaFactory, StreamSource schemaSource) {
    final SchemaHelper schemaHelper = SchemaHelper.schemaHelper(schemaFactory);
    final Schema schema = schemaHelper.asSchema(schemaSource);
    return schemaHelper.conformityChecker(schema);
  }
}
