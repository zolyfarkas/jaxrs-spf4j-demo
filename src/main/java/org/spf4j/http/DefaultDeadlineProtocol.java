
package org.spf4j.http;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.spf4j.base.CharSequences;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;

/**
 * @author Zoltan Farkas
 */
public class DefaultDeadlineProtocol implements DeadlineProtocol {

  private final String deadlineHeaderName;

  private final String timeoutHeaderName;

  private final long defaultTimeoutNanos;

  private final long maxTimeoutNanos;

  public DefaultDeadlineProtocol() {
    this(Headers.REQ_DEADLINE, Headers.REQ_TIMEOUT, 60000000000L, 600000000000L);
  }

  public DefaultDeadlineProtocol(String deadlineHeaderName, String timeoutHeaderName,
          final long defaultTimeoutNanos, final long maxTimeoutNanos) {
    if (defaultTimeoutNanos > maxTimeoutNanos) {
      throw new IllegalArgumentException("Invalid server configuration,"
              + " default timeout must be smaller than max timeout " + defaultTimeoutNanos + " < " + maxTimeoutNanos);
    }
    this.deadlineHeaderName = deadlineHeaderName;
    this.timeoutHeaderName = timeoutHeaderName;
    this.defaultTimeoutNanos = defaultTimeoutNanos;
    this.maxTimeoutNanos = maxTimeoutNanos;
  }

  @Override
  public long serialize(final  BiConsumer<String, String> headers, final long deadlineNanos) {
    long timeoutNanos = deadlineNanos - TimeSource.nanoTime();
    Instant deadline = Timing.getCurrentTiming().fromNanoTimeToInstant(deadlineNanos);
    headers.accept(deadlineHeaderName, Long.toString(deadline.getEpochSecond())  + ' ' + deadline.getNano());
    headers.accept(timeoutHeaderName, timeoutNanos + " n");
    return timeoutNanos;
  }


  @Override
  public long deserialize(Function<String, String> headers, final long currentTimeNanos) {
    String deadlineStr = headers.apply(deadlineHeaderName);
    long deadlineNanos;
    if (deadlineStr == null) {
      String timeoutStr =  headers.apply(timeoutHeaderName);
      if (timeoutStr == null) {
        deadlineNanos = currentTimeNanos + defaultTimeoutNanos;
      } else {
        long parseTimeoutNanos = parseTimeoutNanos(timeoutStr);
        if (parseTimeoutNanos > maxTimeoutNanos) {
            Logger.getLogger(DefaultDeadlineProtocol.class.getName())
                    .log(Level.WARNING, "Overwriting client supplied timeout {0} ns with {1} ns",
                            new Object [] {parseTimeoutNanos, maxTimeoutNanos});
            deadlineNanos = currentTimeNanos + maxTimeoutNanos;
        } else {
          deadlineNanos = currentTimeNanos + parseTimeoutNanos;
        }
      }
    } else {
      deadlineNanos = parseDeadlineNanos(deadlineStr);
      long timeoutNanos = deadlineNanos - currentTimeNanos;
      if (timeoutNanos > maxTimeoutNanos) {
        Logger.getLogger(DefaultDeadlineProtocol.class.getName())
                .log(Level.WARNING, "Overwriting client supplied timeout {0} ns with {1} ns",
                        new Object [] {timeoutNanos, maxTimeoutNanos});
        deadlineNanos = currentTimeNanos + maxTimeoutNanos;
      }
    }
    return deadlineNanos;
  }


/**
   Hour → "H"
   Minute → "M"
   Second → "S"
   Millisecond → "m"
   Microsecond → "u"
   Nanosecond → "n"
  */
   public static TimeUnit from(final CharSequence cs, final int idx) {
     switch (cs.charAt(idx)) {
       case 'H':
         return TimeUnit.HOURS;
       case 'M':
         return TimeUnit.MINUTES;
       case 'S':
         return TimeUnit.SECONDS;
       case 'm':
         return TimeUnit.MILLISECONDS;
       case 'u':
         return TimeUnit.MICROSECONDS;
       case 'n':
         return TimeUnit.NANOSECONDS;
       default:
         throw new IllegalArgumentException("Unsupported time unit " + cs + " at " + idx);
     }
   }

   public static long parseDeadlineNanos(final CharSequence deadlineHeaderValue) {
    long deadlineSeconds = CharSequences.parseUnsignedLong(deadlineHeaderValue, 10, 0);
    long nanoTime = Timing.getCurrentTiming().fromEpochMillisToNanoTime(deadlineSeconds * 1000);
    int indexOf = CharSequences.indexOf(deadlineHeaderValue, 0, deadlineHeaderValue.length(), ' ');
    if (indexOf < 0) {
      return nanoTime;
    } else {
      return nanoTime + CharSequences.parseUnsignedInt(deadlineHeaderValue, 10, indexOf + 1);
    }
  }

  public static long parseTimeoutNanos(final CharSequence timeoutHeaderValue) {
     long timeoutValue = CharSequences.parseUnsignedLong(timeoutHeaderValue, 10, 0);
     int indexOf = CharSequences.indexOf(timeoutHeaderValue, 0, timeoutHeaderValue.length(), ' ');
     if (indexOf < 0) {
       return TimeUnit.MILLISECONDS.toNanos(timeoutValue);
     }
     TimeUnit unit = from(timeoutHeaderValue, indexOf + 1);
     return unit.toNanos(timeoutValue);
  }


}
