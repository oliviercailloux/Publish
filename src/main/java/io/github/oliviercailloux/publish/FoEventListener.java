package io.github.oliviercailloux.publish;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.github.oliviercailloux.jaris.xml.XmlException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.transform.TransformerException;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

class FoEventListener implements EventListener {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(FoEventListener.class);

  private final ArrayList<Event> events;

  FoEventListener() {
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
        throw new VerifyException("Unexpected event severity: %s.".formatted(event.getSeverity()));
      }
      LoggingEventBuilder builder = FoToPdfTransformer.LOGGER.atLevel(level);
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
    if (e.getParam("e") instanceof Exception x) {
      throw new XmlException(new TransformerException(x));
    }
    throw new XmlException(new TransformerException(e.toString()));
  }
}
