package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import io.github.oliviercailloux.jaris.xml.SchemaHelper;
import io.github.oliviercailloux.jaris.xml.XmlException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.SchemaOutputResolver;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

public class JaxbHelper {
  public static final Unchecker<JAXBException, XmlException> JAXB_EXCEPTION_TO_XML_EXCEPTION =
      Unchecker.wrappingWith(XmlException::new);

  public static final Unchecker<JAXBException, XmlException> JAXB_EXPECTION_TO_VERIFY_EXCEPTION =
      Unchecker.wrappingWith(XmlException::new);

  /**
   * Thanks to https://stackoverflow.com/a/7853466/.
   */
  private static class InMemoryResolver extends SchemaOutputResolver {
    private final List<DOMResult> results = new ArrayList<>();

    @Override
    public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
      DOMResult result = new DOMResult();
      result.setSystemId(suggestedFileName);
      results.add(result);
      return result;
    }

    @SuppressWarnings("unused")
    public ImmutableList<DOMResult> getResults() {
      return ImmutableList.copyOf(results);
    }

    public DOMSource getResultAsSource() {
      if (results.size() >= 2) {
        throw new XmlException("More than one schema result.");
      }
      if (results.isEmpty()) {
        throw new XmlException("No schema result.");
      }
      return Iterables.getOnlyElement(getResultsAsSources());
    }

    public ImmutableList<DOMSource> getResultsAsSources() {
      return results.stream().map(DOMResult::getNode).map(DOMSource::new)
          .collect(ImmutableList.toImmutableList());
    }
  }

  private JAXBContext context;
  private Schema schema;

  public static JaxbHelper using(JAXBContext context) {
    return new JaxbHelper(context);
  }

  public static JaxbHelper newContext(Class<?>... classes) throws XmlException {
    return new JaxbHelper(
        JAXB_EXCEPTION_TO_XML_EXCEPTION.getUsing(() -> JAXBContext.newInstance(classes)));
  }

  private JaxbHelper(JAXBContext context) {
    this.context = checkNotNull(context);
    schema = null;
  }

  public JAXBContext getContext() {
    return context;
  }

  private Schema lazyGetSchema() {
    if (schema != null) {
      final DOMSource generated;
      {
        final InMemoryResolver schemasHolder = new InMemoryResolver();
        try {
          context.generateSchema(schemasHolder);
        } catch (IOException e) {
          throw new VerifyException(e);
        }
        generated = schemasHolder.getResultAsSource();
      }
      schema = SchemaHelper.schemaHelper().asSchema(generated);
    }
    return schema;
  }

  public Schema getSchema() {
    return lazyGetSchema();
  }

  /**
   * Transforms the given element into its XML representation.
   *
   * @param element the element
   * @return serialized xml
   * @throws XmlException iff any unexpected problem occurs obtaining the marshaller or during the
   *         marshalling.
   */
  public String toXml(JAXBElement<?> element) throws XmlException {
    /* Could also try to use Transform with a Jaxbsource. */
    final Marshaller marshaller =
        JAXB_EXCEPTION_TO_XML_EXCEPTION.getUsing(() -> context.createMarshaller());
    JAXB_EXPECTION_TO_VERIFY_EXCEPTION
        .call(() -> marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE));
    marshaller.setSchema(lazyGetSchema());
    final StringWriter wr = new StringWriter();
    JAXB_EXCEPTION_TO_XML_EXCEPTION.call(() -> marshaller.marshal(element, wr));
    return wr.toString();
  }
}
