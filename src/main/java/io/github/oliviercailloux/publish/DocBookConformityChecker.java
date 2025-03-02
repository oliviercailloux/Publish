package io.github.oliviercailloux.publish;

import com.google.common.base.VerifyException;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.SchemaHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocBookConformityChecker {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookConformityChecker.class);

  public static final CharSource SCHEMA_SOURCE =
      Resources.asCharSource(DocBookConformityChecker.class.getResource("docbook.rng"), StandardCharsets.UTF_8);

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
    } catch (XmlException | IOException e) {
      throw new VerifyException(e);
    }
    return cc;
  }

  static ConformityChecker cc(SchemaFactory schemaFactory, CharSource schemaSource)
      throws XmlException, IOException {
    final SchemaHelper schemaHelper = SchemaHelper.schemaHelper(schemaFactory);
    final Schema schema = schemaHelper.asSchema(schemaSource);
    return schemaHelper.conformityChecker(schema);
  }
}
