package io.github.oliviercailloux.publish;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import io.github.oliviercailloux.jaris.xml.XmlException;
import io.github.oliviercailloux.jaris.xml.XmlToBytesTransformer;
import io.github.oliviercailloux.jaris.xml.XmlTransformerFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;
import org.apache.fop.fo.FOTreeBuilder;
import org.apache.xmlgraphics.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
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
        Collection<Object> pValues = event.getParams().values();
        ImmutableSet<Exception> exceptions = pValues.stream().filter(Exception.class::isInstance)
            .map(Exception.class::cast).collect(ImmutableSet.toImmutableSet());
        final Level level;
        if (event.getSeverity() == EventSeverity.INFO) {
          level = Level.DEBUG;
        } else if (event.getSeverity() == EventSeverity.WARN) {
          level = Level.WARN;
        } else if (event.getSeverity() == EventSeverity.ERROR) {
          level = Level.ERROR;
        } else if (event.getSeverity() == EventSeverity.FATAL) {
          level = Level.ERROR;
        } else {
          throw new VerifyException(
              "Unexpected event severity: %s.".formatted(event.getSeverity()));
        }
        LoggingEventBuilder builder = LOGGER.atLevel(level);
        if (exceptions.isEmpty()) {
          String formatted = "Event severity %s: %s.".formatted(event.getSeverity(), msg);
          builder.setMessage(formatted);
          builder.log();
        } else if (exceptions.size() == 1) {
          String formatted = "Event severity %s: %s.".formatted(event.getSeverity(), msg);
          builder.setMessage(formatted);
          Exception e = Iterables.getOnlyElement(exceptions);
          builder.setCause(e);
          builder.log();
        } else {
          String formatted = "Event severity %s: %s, and %s exceptions."
              .formatted(event.getSeverity(), msg, exceptions.size());
          builder.setMessage(formatted);
          builder.log();
          for (Exception e : exceptions) {
            LOGGER.atLevel(level).setCause(e).log();
          }
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

  private static class FoToBytesTransformer implements XmlToBytesTransformer {
    private final FopFactory fopFactory;
    private final XmlTransformerFactory delegateTransformer;

    private FoToBytesTransformer(FopFactory fopFactory, XmlTransformerFactory delegateTransformer) {
      this.fopFactory = fopFactory;
      this.delegateTransformer = delegateTransformer;
    }

    @Override
    public void sourceToResult(Source source, Result result) throws XmlException {
      checkArgument(result instanceof StreamResult);
      final StreamResult streamResult = (StreamResult) result;

      final FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

      final FoEventListener l = new FoEventListener();
      foUserAgent.getEventBroadcaster().addEventListener(l);

      final Result res;
      try (OutputStream out = streamResult.getOutputStream()) {
        res = new SAXResult(new FOTreeBuilder(MimeConstants.MIME_PDF, foUserAgent, out));
      } catch (FOPException e) {
        throw new IllegalStateException(e);
      } catch (IOException e) {
        throw new XmlException(e);
      }

      delegateTransformer.usingEmptyStylesheet().sourceToResult(source, res);
      /*
       * This duplicates the serious event that will get thrown in the log, but we’d better do that
       * so that one can see the order of events in the log and thus where the first serious one
       * happened exactly.
       */
      LOGGER.debug("Got {} serious and {} not serious events.", l.seriouses().size(),
          l.notSeriouses().size());
      l.logAll();
      l.seriouses().stream().findFirst().ifPresent(e -> {
        throw FoEventListener.asException(e);
      });
    }
  }

  public static XmlToBytesTransformer usingFactory(TransformerFactory factory) {
    final FopFactory fopFactory = getFopFactory();
    final XmlTransformerFactory transformer = XmlTransformerFactory.usingFactory(factory).pedantic();

    return new FoToBytesTransformer(fopFactory, transformer);
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a transformer
   */
  public static XmlToBytesTransformer usingFactory(TransformerFactory factory, URI fopBaseUri) {
    final FopFactory fopFactory = getFopFactory(fopBaseUri);
    final XmlTransformerFactory transformer = XmlTransformerFactory.usingFactory(factory).pedantic();

    return new FoToBytesTransformer(fopFactory, transformer);
  }

  /**
   * @param fopBaseUri the absolute base URI used by FOP to resolve resource URIs against
   * @return a transformer
   */
  public static XmlToBytesTransformer usingFactory(TransformerFactory factory, URI fopBaseUri,
      ByteSource config) throws XmlException, IOException {
    FopFactory fopFactory;
    try {
      fopFactory = getFopFactory(fopBaseUri, config);
    } catch (SAXException e) {
      throw new XmlException(e);
    }
    final XmlTransformerFactory transformer = XmlTransformerFactory.usingFactory(factory).pedantic();

    return new FoToBytesTransformer(fopFactory, transformer);
  }

  public static XmlToBytesTransformer usingFactory(TransformerFactory factory, FopConfParser config)
      throws XmlException {
    FopFactory fopFactory = getFopFactory(config);
    final XmlTransformerFactory transformer = XmlTransformerFactory.usingFactory(factory).pedantic();

    return new FoToBytesTransformer(fopFactory, transformer);
  }

  private static FopFactory getFopFactory() {
    final URL configUrl = FoToPdfTransformer.class.getResource("fop-config.xml");
    final URI base = Unchecker.URI_UNCHECKER.getUsing(() -> configUrl.toURI().resolve(".."));
    try {
      return getFopFactory(base, Resources.asByteSource(configUrl));
    } catch (SAXException | IOException e) {
      throw new VerifyException(e);
    }
  }

  private static FopFactory getFopFactory(URI fopBaseUri) {
    final URL configUrl = FoToPdfTransformer.class.getResource("fop-config.xml");
    try {
      return getFopFactory(fopBaseUri, Resources.asByteSource(configUrl));
    } catch (SAXException | IOException e) {
      throw new VerifyException(e);
    }
  }

  /**
   * @param fopBaseUri the base URI to resolve resource URIs against
   * @param configUrl the FOP conf
   * @return a fop factory
   * @throws SAXException iff a SAX error was thrown parsing the FOP conf
   * @throws IOException iff an I/O error is thrown while parsing the FOP conf
   */
  private static FopFactory getFopFactory(URI fopBaseUri, ByteSource config)
      throws SAXException, IOException {
    checkArgument(fopBaseUri.isAbsolute());

    final FopFactory fopFactory;
    try (InputStream configStream = config.openBufferedStream()) {
      fopFactory = FopFactory.newInstance(fopBaseUri, configStream);
    }
    checkArgument(fopFactory.validateUserConfigStrictly());
    checkArgument(fopFactory.validateStrictly());

    return fopFactory;
  }

  private static FopFactory getFopFactory(FopConfParser config) {
    final FopFactory fopFactory = config.getFopFactoryBuilder().setStrictFOValidation(true)
        .setStrictUserConfigValidation(true).build();
    verify(fopFactory.validateUserConfigStrictly());
    verify(fopFactory.validateStrictly());

    return fopFactory;
  }
}
