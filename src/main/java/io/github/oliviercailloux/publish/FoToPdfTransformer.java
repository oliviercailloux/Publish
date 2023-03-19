package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlTransformer;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;
import org.apache.xmlgraphics.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class FoToPdfTransformer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FoToPdfTransformer.class);

  private static class FoEventListener implements EventListener {

    private final ArrayList<Event> events;

    private FoEventListener() {
      events = new ArrayList<>();
    }

    @Override
    public void processEvent(Event event) {
      /* https://xmlgraphics.apache.org/fop/2.4/events.html */
      events.add(event);
    }

    @SuppressWarnings("unused")
    public ImmutableList<Event> notSeriouses() {
      return events.stream().filter(e -> e.getSeverity().equals(EventSeverity.INFO))
          .collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<Event> seriouses() {
      return events.stream().filter(e -> !e.getSeverity().equals(EventSeverity.INFO))
          .collect(ImmutableList.toImmutableList());
    }

    public void logAll() {
      for (Event event : events) {
        final String msg = EventFormatter.format(event);
        if (event.getSeverity() == EventSeverity.INFO) {
          LOGGER.debug("Informative event: {}.", msg);
        } else if (event.getSeverity() == EventSeverity.WARN) {
          LOGGER.warn("Warning event: {}.", msg);
        } else if (event.getSeverity() == EventSeverity.ERROR) {
          LOGGER.error("Error event: {}.", msg);
        } else if (event.getSeverity() == EventSeverity.FATAL) {
          LOGGER.error("Fatal event: {}.", msg);
        }
      }
    }

    public static XmlException asException(Event e) {
      if (e.getParam("fnfe") instanceof FileNotFoundException fnfe) {
        throw new XmlException(new TransformerException(fnfe));
      }
      throw new XmlException(new TransformerException(e.toString()));
    }
  }

  private static class FoToBytesTransformer implements ToBytesTransformer {
    private final FopFactory fopFactory;
    private final XmlTransformer delegateTransformer;

    private FoToBytesTransformer(FopFactory fopFactory, XmlTransformer delegateTransformer) {
      this.fopFactory = fopFactory;
      this.delegateTransformer = delegateTransformer;
    }

    @Override
    public void toStream(Source source, OutputStream destination) throws XmlException {
      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
      // foUserAgent.getEventBroadcaster().addEventListener(new LoggingEventListener());

      final FoEventListener l = new FoEventListener();
      foUserAgent.getEventBroadcaster().addEventListener(l);

      final Result res;
      try {
        final Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, destination);
        res = new SAXResult(fop.getDefaultHandler());
      } catch (FOPException e) {
        throw new IllegalStateException(e);
      }

      delegateTransformer.usingEmptySource().transform(source, res);
      /*
       * This duplicates the serious event that will get thrown in the log, but we’d better do that
       * so that one can see the order of events in the log and thus where the first serious one
       * happened exactly.
       */
      l.logAll();
      l.seriouses().stream().findFirst().ifPresent(e -> {
        throw FoEventListener.asException(e);
      });
    }
  }

  public static FoToPdfTransformer usingDefaultFactory() {
    /* FIXME decide on the default. */
    final TransformerFactory factory = TransformerFactory.newDefaultInstance();
    // return new DocBookTransformer(new org.apache.xalan.processor.TransformerFactoryImpl());
    return new FoToPdfTransformer(factory);
  }

  public static FoToPdfTransformer usingFactory(TransformerFactory factory) {
    return new FoToPdfTransformer(factory);
  }

  private final XmlTransformer transformer;

  private FoToPdfTransformer(TransformerFactory transformerFactory) {
    checkNotNull(transformerFactory);
    transformer = XmlTransformer.usingFactory(transformerFactory);
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a fop factory
   * @throws IOException iff an error occurs while reading the resources required by fop factory
   */
  private FopFactory getFopFactory(URI fopBaseUri) throws IOException {
    checkArgument(fopBaseUri.isAbsolute());
    final URL configUrl = DocBookTransformer.class.getResource("fop-config.xml");

    final FopFactory fopFactory;
    try (InputStream configStream = configUrl.openStream()) {
      fopFactory = FopFactory.newInstance(fopBaseUri, configStream);
    } catch (SAXException e) {
      throw new VerifyException(e);
    }
    verify(fopFactory.validateStrictly());
    verify(fopFactory.validateUserConfigStrictly());

    return fopFactory;
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a transformer
   * @throws IOException iff an error occurs while reading the fop factory required resources
   */
  public ToBytesTransformer usingBaseUri(URI fopBaseUri) throws IOException {
    final FopFactory fopFactory = getFopFactory(fopBaseUri);

    return new FoToBytesTransformer(fopFactory, transformer);
  }
}
