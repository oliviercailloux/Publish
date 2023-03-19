package io.github.oliviercailloux.publish;

import com.google.common.base.VerifyException;
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

  public static final StreamSource SCHEMA_SOURCE =
      new StreamSource(DocBookConformityChecker.class.getResource("docbook.rng").toString());

  /**
   * Provides a DocBook conformity checker that uses the default factory and the embedded RNG
   * DocBook schema.
   * <p>
   * It is recommended to embed your own RNG schema and use the following code for deciding which
   * schema is used (and which factory). {@code
   * SchemaHelper schemaHelper = SchemaHelper.schemaHelper(schemaFactory);
   * Schema schema = schemaHelper.asSchema(schemaSource);
   * return schemaHelper.conformityChecker(schema);
   * }
   *
   * @return a conformity checker
   * @see SchemaHelper
   * @see XMLSyntaxSchemaFactory
   */
  public static ConformityChecker usingEmbeddedSchema() {
    final ConformityChecker cc;
    try {
      cc = cc(new XMLSyntaxSchemaFactory(), SCHEMA_SOURCE);
    } catch (XmlException e) {
      throw new VerifyException(e);
    }
    return cc;
  }

  static ConformityChecker cc(SchemaFactory schemaFactory, StreamSource schemaSource)
      throws XmlException {
    final SchemaHelper schemaHelper = SchemaHelper.schemaHelper(schemaFactory);
    final Schema schema = schemaHelper.asSchema(schemaSource);
    return schemaHelper.conformityChecker(schema);
  }
}
