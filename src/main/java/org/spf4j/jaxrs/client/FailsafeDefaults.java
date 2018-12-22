package org.spf4j.jaxrs.client;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.failsafe.concurrent.DefaultRetryExecutor;

/**
 * @author Zoltan Farkas
 */
public final class FailsafeDefaults {

  private FailsafeDefaults() { }

  private static final AsyncRetryExecutor<Object, Callable<? extends Object>> DEFAULT_HTTP_RETRY_EXEC
          = RetryPolicy.newBuilder()
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
                  .build()
                  .async(HedgePolicy.DEFAULT, DefaultRetryExecutor.instance());

  public static AsyncRetryExecutor<Object, Callable<? extends Object>> defaultExecutor() {
    return DEFAULT_HTTP_RETRY_EXEC;
  }

}
