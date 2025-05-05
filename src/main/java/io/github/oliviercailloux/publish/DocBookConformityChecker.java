package io.github.oliviercailloux.publish;

import com.google.common.base.VerifyException;
import com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory;
import io.github.oliviercailloux.docbook.DocBookResources;
import io.github.oliviercailloux.jaris.xml.ConformityChecker;
import io.github.oliviercailloux.jaris.xml.SchemaHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.IOException;
import java.net.URI;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocBookConformityChecker {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DocBookConformityChecker.class);

  /**
   * Provides a DocBook conformity checker that uses the default factory and the embedded RNG 5.1
   * DocBook schema.
   *
   * @return a conformity checker
   * @see SchemaHelper
   * @see XMLSyntaxSchemaFactory
   */
  public static ConformityChecker usingEmbeddedSchema() {
    try {
      return cc(new XMLSyntaxSchemaFactory(), DocBookResources.RNG_5_1_URI);
    } catch (XmlException e) {
      throw new VerifyException(e);
    }
  }

  static ConformityChecker cc(SchemaFactory schemaFactory, URI schemaSource) throws XmlException {
    final SchemaHelper schemaHelper = SchemaHelper.schemaHelper(schemaFactory);
    final Schema schema = schemaHelper.asSchema(schemaSource);
    return schemaHelper.conformityChecker(schema);
  }
}
