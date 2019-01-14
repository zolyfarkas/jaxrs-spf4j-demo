package org.spf4j.http;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author Zoltan Farkas
 */
public interface DeadlineProtocol {

  /**
   *
   * @param headers
   * @param currentTimeNanos
   * @return the deadline in nanoseconds.
   */
  long deserialize(Function<String, String> headers, long currentTimeNanos);


  /**
   * @param headers
   * @param deadlineNanos
   * @return the timeout in nanoseconds.
   */
  long serialize(BiConsumer<String, String> headers, long deadlineNanos);

}
