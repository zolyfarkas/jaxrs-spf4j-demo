
package org.spf4j.actuator.logs;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.TimeoutHandler;

/**
 *
 * @author Zoltan Farkas
 */
public class AsyncResponseWrapper implements AsyncResponse {

  private final AsyncResponse resp;

  public AsyncResponseWrapper(final AsyncResponse resp) {
    this.resp = resp;
  }

  @Override
  public boolean resume(Object response) {
    return resp.resume(response);
  }

  @Override
  public boolean resume(Throwable response) {
    return resp.resume(response);
  }

  @Override
  public boolean cancel() {
    return resp.cancel();
  }

  @Override
  public boolean cancel(int retryAfter) {
    return resp.cancel(retryAfter);
  }

  @Override
  public boolean cancel(Date retryAfter) {
    return resp.cancel(retryAfter);

  }

  @Override
  public boolean isSuspended() {
    return resp.isSuspended();
  }

  @Override
  public boolean isCancelled() {
    return resp.isCancelled();
  }

  @Override
  public boolean isDone() {
    return resp.isDone();
  }

  @Override
  public boolean setTimeout(long time, TimeUnit unit) {
    return resp.setTimeout(time, unit);
  }

  @Override
  public void setTimeoutHandler(TimeoutHandler handler) {
    resp.setTimeoutHandler(handler);
  }

  @Override
  public Collection<Class<?>> register(Class<?> callback) {
    return resp.register(callback);
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) {
    return resp.register(callback, callbacks);
  }

  @Override
  public Collection<Class<?>> register(Object callback) {
    return register(callback);
  }

  @Override
  public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) {
    return register(callback, callbacks);
  }

}
