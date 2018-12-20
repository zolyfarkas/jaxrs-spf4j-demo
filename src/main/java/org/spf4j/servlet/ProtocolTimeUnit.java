
package org.spf4j.servlet;

import java.util.concurrent.TimeUnit;
import org.spf4j.base.CharSequences;
import org.spf4j.base.Timing;

/**
 * @author Zoltan Farkas
 */
public class ProtocolTimeUnit {

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
