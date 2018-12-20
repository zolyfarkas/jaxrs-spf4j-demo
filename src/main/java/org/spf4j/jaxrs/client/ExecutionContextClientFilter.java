package org.spf4j.jaxrs.client;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
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
 * @author Pavol Loffay
 */
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public class ExecutionContextClientFilter implements ClientRequestFilter, ClientResponseFilter {

  private static final Logger log = Logger.getLogger("org.spf4j.jaxrs.client");

  @Override
  public void filter(ClientRequestContext requestContext) {
    ExecutionContext current = ExecutionContexts.current();
    ExecutionContext reqCtx = current.startChild(requestContext.getMethod() + ' ' + requestContext.getUri().getPath());
    MultivaluedMap<String, Object> headers = requestContext.getHeaders();
    long deadlineNanos = reqCtx.getDeadlineNanos();
    Instant deadline = Timing.getCurrentTiming().fromNanoTimeToInstant(deadlineNanos);
    headers.add(Headers.REQ_DEADLINE, deadline.getEpochSecond()  + ' ' + deadline.getNano());
    long timeoutNanos  = deadlineNanos - TimeSource.nanoTime();
    if (timeoutNanos <=  0) {
      reqCtx.close();
      throw new UncheckedTimeoutException("timeout  is "  + timeoutNanos +  "ns");
    }
    headers.add(Headers.REQ_TIMEOUT, timeoutNanos + " n");
    headers.add(Headers.REQ_ID, reqCtx.getId());
    requestContext.setProperty("XCTX", reqCtx);
    requestContext.setProperty(ClientProperties.READ_TIMEOUT, (int) (timeoutNanos / 1000000));
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
    ExecutionContext reqCtx = (ExecutionContext) requestContext.getProperty("XCTX");
    reqCtx.close();
    if (log.isLoggable(Level.FINE)) {
      log.log(Level.FINE, "Done {0}", new Object[] {reqCtx.getName(),
        LogAttribute.traceId(reqCtx.getId()),
        LogAttribute.execTimeMicros(TimeSource.nanoTime() - reqCtx.getStartTimeNanos(), TimeUnit.NANOSECONDS)});
    }
  }

}
