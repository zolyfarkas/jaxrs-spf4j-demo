package org.spf4j.http;

/**
 * default HTTP headers.
 * @author Zoltan Farkas
 */
public final class Headers {

  /**
   * request-deadline → SecondsSinceEpoch Nanos?
   */
  public static final String REQ_DEADLINE = "request-deadline";

  /**
   * request-timeout → TimeoutValue TimeoutUnit?
   *  TimeoutValue → {positive integer as ASCII string of at most 8 digits}
   *  TimeoutUnit → Hour / Minute / Second / Millisecond / Microsecond / Nanosecond
   *  Hour → "H"
   *  Minute → "M"
   *  Second → "S"
   *  Millisecond → "m"
   *  Microsecond → "u"
   *  Nanosecond → "n"
   */
  public static final String REQ_TIMEOUT = "request-timeout";

  /**
   * A ID in the form of [chars]/[chars] where / delimits the execution context hierarchy.
   */
  public static final String REQ_ID = "request-id";

  public static final String CONTENT_SCHEMA = "content-schema";

  public static final String WARNING = "Warning";

}
