package org.spf4j.jaxrs;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckReturnValue;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.Throwables;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.http.Headers;
import org.spf4j.log.AvroLogRecordImpl;

/**
 * @author Zoltan Farkas
 */
public final class Utils {

  public static final RetryPolicy DEFAULT_HTTP_RETRY_POLICY = RetryPolicy.newBuilder()
                  .withDefaultThrowableRetryPredicate()
                  .withExceptionPartialPredicate(WebApplicationException.class,
                          (WebApplicationException ex, Callable<? extends Object> c) -> {
                            Response response = ex.getResponse();
                            int status = response.getStatus();
                            switch (status) {
                              case 408:
                              case 409:
                              case 419:
                              case 420:
                              case 423:
                              case 429:
                              case 440:
                              case 449:
                              case 503:
                              case 504:
                              case 509:
                              case 522:
                              case 524:
                              case 599:
                                String retryAfter = response.getHeaderString("Retry-After");
                                if (retryAfter != null && !retryAfter.isEmpty()) {
                                  if (Character.isDigit(retryAfter.charAt(0))) {
                                    RetryDecision.retry(Long.parseLong(retryAfter), TimeUnit.SECONDS, c);
                                  } else {
                                    RetryDecision.retry(Duration.between(Instant.now(),
                                            DateTimeFormatter.RFC_1123_DATE_TIME.parse(retryAfter, Instant::from)).toNanos(),
                                            TimeUnit.NANOSECONDS, c);
                                  }
                                }
                                return RetryDecision.retryDefault(c);
                              default:
                                if (status >= 400 && status < 500) {
                                  return RetryDecision.abort();
                                }
                            }
                            return null;
                          })
                  .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
                  .build();

  private Utils() { }

  public static RetryPolicy defaultRetryPolicy() {
    return DEFAULT_HTTP_RETRY_POLICY;
  }

  @CheckReturnValue
  public static  void handleServiceError(final WebApplicationException ex,
          final ExecutionContext current) throws WebApplicationException {
    Response response = ex.getResponse();
    if (response.getHeaders().getFirst(Headers.CONTENT_SCHEMA) == null) {
      return;
    }
    ServiceError se;
    try {
      se = response.readEntity(ServiceError.class);
    } catch (ProcessingException e) {
      // not a Propagable service error.
      ex.addSuppressed(e);
      return;
    }
    DebugDetail detail = se.getDetail();
    if (detail != null) {
      org.spf4j.base.avro.Throwable throwable = detail.getThrowable();
      if (throwable != null) {
        Throwables.setRootCause(ex, Converters.convert(detail.getOrigin(), throwable));
      }
      if (current != null) {
        for (LogRecord log : detail.getLogs()) {
          current.addLog(new AvroLogRecordImpl(log));
        }
      }
    }
  }


  public static int getIntConfigValue(final Configuration cfg, final String cfgKey, final int defaultValue) {
    Number nr = (Number) cfg.getProperty(cfgKey);
    if (nr == null) {
      return Integer.getInteger(cfgKey, defaultValue);
    } else {
      return nr.intValue();
    }
  }

  public static String getStringConfigValue(final Configuration cfg, final String cfgKey, final String defaultValue) {
    String val = (String) cfg.getProperty(cfgKey);
    if (val == null) {
      return System.getProperty(cfgKey, defaultValue);
    } else {
      return val;
    }
  }

}
