package org.spf4j.jaxrs.client;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientProperties;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.base.Timing;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.http.Headers;
import org.spf4j.log.LogAttribute;

/**
 * A client filter for setting the following HTTP timeout headers:
 * deadline
 * timeout
 * requestId
 *
 * @author Zoltan farkas
 */
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public class ExecutionContextClientFilter implements ClientRequestFilter,
        ClientResponseFilter {

  private static final Logger log = Logger.getLogger("org.spf4j.jaxrs.client");


  @Override
  public void filter(final ClientRequestContext requestContext) {
    ExecutionContext reqCtx = ExecutionContexts.current();
    MultivaluedMap<String, Object> headers = requestContext.getHeaders();
    long timeoutNanos;
    try {
      timeoutNanos = reqCtx.getTimeToDeadline(TimeUnit.NANOSECONDS);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    }
    long deadlineNanos = reqCtx.getDeadlineNanos();
    Instant deadline = Timing.getCurrentTiming().fromNanoTimeToInstant(deadlineNanos);
    headers.add(Headers.REQ_DEADLINE, Long.toString(deadline.getEpochSecond())  + ' ' + deadline.getNano());
    headers.add(Headers.REQ_TIMEOUT, timeoutNanos + " n");
    headers.add(Headers.REQ_ID, reqCtx.getId());
    int readTimeoutMs = (int) (timeoutNanos / 1000000);
    requestContext.setProperty(ClientProperties.READ_TIMEOUT, readTimeoutMs);
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, "Invoking {0}", new Object[] {reqCtx.getName(), LogAttribute.of("headers", headers)});
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
    if (log.isLoggable(Level.FINE)) {
      ExecutionContext reqCtx = ExecutionContexts.current();
      log.log(Level.FINE, "Done {0}", new Object[] {reqCtx.getName(),
        LogAttribute.traceId(reqCtx.getId()),
        LogAttribute.value("httpStatus", responseContext.getStatus()),
        LogAttribute.execTimeMicros(TimeSource.nanoTime() - reqCtx.getStartTimeNanos(), TimeUnit.NANOSECONDS)});
    }
  }

}
