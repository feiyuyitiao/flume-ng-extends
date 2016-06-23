
package com.github.ningg.flume.source;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.annotations.InterfaceAudience;
import org.apache.flume.annotations.InterfaceStability;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.serialization.EventDeserializer;
import org.apache.flume.serialization.ResettableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Ascii;
import com.google.common.collect.Lists;

/**
 * A deserializer that parses text lines from a file.
 */
@InterfaceAudience.Private
@InterfaceStability.Evolving
public class WrapLineDeserializer implements EventDeserializer {

  private static final Logger logger = LoggerFactory.getLogger
      (WrapLineDeserializer.class);

  private final ResettableInputStream in;
  private final Charset outputCharset;
  private final int maxLineLength;
  private volatile boolean isOpen;

  public static final String OUT_CHARSET_KEY = "outputCharset";
  public static final String CHARSET_DFLT = "UTF-8";

  public static final String MAXLINE_KEY = "maxLineLength";
  public static final int MAXLINE_DFLT = 2048;

  WrapLineDeserializer(Context context, ResettableInputStream in) {
    this.in = in;
    this.outputCharset = Charset.forName(
        context.getString(OUT_CHARSET_KEY, CHARSET_DFLT));
    this.maxLineLength = context.getInteger(MAXLINE_KEY, MAXLINE_DFLT);
    this.isOpen = true;
  }

  /**
   * Reads a line from a file and returns an event
   * 
   *  my description: 
   *  change to if log line contains assigned char, and next line didn't wrap the previous line.
   * @return Event containing parsed line
   * @throws IOException
   */
  @Override
  public Event readEvent() throws IOException {
    ensureOpen();
    String line = readLine();
//    logger.error("line:{}", line);
    if (line == null) {
      return null;
    } else {
    	//wrap line if match condition.
    	if(line.length()>=5) {
    		String lineStarter = line.substring(0,5 );
    		if(lineStarter.contains("=[")) {
    			String line2 = readLine();
    			return EventBuilder.withBody(line + line2, outputCharset);
    		}
    	}
    	return EventBuilder.withBody(line, outputCharset);
    }
  }

  /**
   * Batch line read
   * @param numEvents Maximum number of events to return.
   * @return List of events containing read lines
   * @throws IOException
   */
  @Override
  public List<Event> readEvents(int numEvents) throws IOException {
//		System.err.println("numEvents:"+ numEvents);
    ensureOpen();
    List<Event> events = Lists.newLinkedList();
    for (int i = 0; i < numEvents; i++) {
//    	logger.info("读到每一批的第{}行:", i);
      Event event = readEvent();
      if (event != null) {
        events.add(event);
      } else {
        break;
      }
    }
    return events;
  }

  @Override
  public void mark() throws IOException {
    ensureOpen();
    in.mark();
  }

  @Override
  public void reset() throws IOException {
    ensureOpen();
    in.reset();
  }

  @Override
  public void close() throws IOException {
    if (isOpen) {
      reset();
      in.close();
      isOpen = false;
    }
  }

  private void ensureOpen() {
    if (!isOpen) {
      throw new IllegalStateException("Serializer has been closed");
    }
  }

  // TODO: consider not returning a final character that is a high surrogate
  // when truncating
  private String readLine() throws IOException {
    StringBuilder sb = new StringBuilder();
    int c;
    int readChars = 0;
//    System.out.println(in.tell());
    while ((c = in.readChar()) != -1) {
      readChars++;
      // FIXME: support \r\n
      if (c == '\n') {
        break;
      }

      sb.append((char)c);

      if (readChars >= maxLineLength) {
        logger.debug("Line length exceeds max ({}), truncating line!",
            maxLineLength);
        break;
      }
    }

    if (readChars > 0) {
      return sb.toString();
    } else {
      return null;
    }
  }

  public static class Builder implements EventDeserializer.Builder {

    @Override
    public EventDeserializer build(Context context, ResettableInputStream in) {
      return new WrapLineDeserializer(context, in);
    }

  }

}
