package org.spf4j.http;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author Zoltan Farkas
 */
public interface DeadlineProtocol {

  long deserialize(Function<String, String> headers, long currentTimeNanos);

  long serialize(BiConsumer<String, String> headers, long deadlineNanos);

}
