package org.spf4j.jaxrs;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import static org.spf4j.base.ExecutionContexts.start;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Wrapper;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.base.avro.StackSamples;
import org.spf4j.failsafe.RetryDecision;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.http.Headers;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.log.Level;

/**
 * @author Zoltan Farkas
 */
public final class Utils {

  private static final ExecContextLogger LOG = new ExecContextLogger(LoggerFactory.getLogger("org.spf4j.jaxrs.client"));

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

  public static <T> Callable<T> propagatingServiceExceptionHandlingCallable(
          final ExecutionContext ctx,
          final Callable<T> callable, @Nullable final String name, final long deadlineNanos,
          final long callableTimeoutNanos) {
    return new PropagatingServiceExceptionHandler(callable, ctx, name, deadlineNanos, callableTimeoutNanos);
  }


  public static  void handleServiceError(final WebApplicationException ex,
          final ExecutionContext current) {
    Response response = ex.getResponse();
    if (response.getHeaders().getFirst(Headers.CONTENT_SCHEMA) == null) {
       return;
    }
    ServiceError se;
    try {
      se = response.readEntity(ServiceError.class);
    } catch (RuntimeException e) {
      // not a Propagable service error.
      ex.addSuppressed(e);
      return;
    }
    LOG.debug("ServiceError: {}", se.getMessage());
    DebugDetail detail = se.getDetail();
    Throwable rootCause = null;
    if (detail != null) {
      org.spf4j.base.avro.Throwable throwable = detail.getThrowable();
      if (throwable != null) {
        rootCause = Converters.convert(detail.getOrigin(), throwable);
      }
      String origin = detail.getOrigin();
      if (current != null) {
        for (LogRecord log : detail.getLogs()) {
          if (log.getOrigin().isEmpty()) {
            log.setOrigin(origin);
          }
          LOG.log(current, Level.DEBUG, log);
        }
        List<StackSampleElement> stackSamples = detail.getStackSamples();
        if (!stackSamples.isEmpty()) {
          LOG.debug("remoteProfileDetail", new StackSamples(stackSamples));
        }
      }
    }
    WebApplicationException nex = new WebApplicationException(rootCause,
            Response.fromResponse(response).entity(se).build());
    nex.setStackTrace(ex.getStackTrace());
    throw nex;
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


  private static final class PropagatingServiceExceptionHandler<T> implements Callable<T>, Wrapper<Callable<T>> {

    private final Callable<T> task;
    private final ExecutionContext current;

    private final String name;

    private final long deadlineNanos;

    private final long callableTimeoutNanos;

    PropagatingServiceExceptionHandler(final Callable<T> task, final ExecutionContext current,
            @Nullable final String name, final long deadlineNanos, final long callableTimeoutNanos) {
      this.task = task;
      this.current = current;
      this.name = name;
      this.deadlineNanos = deadlineNanos;
      this.callableTimeoutNanos = callableTimeoutNanos;
    }

    @Override
    public T call() throws Exception {
      long aDeadlineNanos;
      if (callableTimeoutNanos < 0) {
        aDeadlineNanos = deadlineNanos;
      }  else {
        aDeadlineNanos = TimeSource.getDeadlineNanos(callableTimeoutNanos, TimeUnit.NANOSECONDS);
        if (aDeadlineNanos > deadlineNanos) {
          aDeadlineNanos = deadlineNanos;
        }
      }
      try (ExecutionContext ctx = start(toString(), current, aDeadlineNanos)) {
        return task.call();
      } catch (Exception ex) {
        Throwable rex = com.google.common.base.Throwables.getRootCause(ex);
        if (rex instanceof WebApplicationException) {
          handleServiceError((WebApplicationException) rex, current);
        }
        throw ex;
      }
    }

    @Override
    public String toString() {
      return  name == null ? task.toString() : name;
    }

    @Override
    public Callable<T> getWrapped() {
      return task;
    }

  }





}
