package org.spf4j.jaxrs.client;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckReturnValue;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.base.Wrapper;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.LogRecord;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.log.AvroLogRecordImpl;

/**
 * @author Zoltan Farkas
 */
public class Spf4jInvocation implements Invocation, Wrapper<Invocation> {

  private final Invocation invocation;
  private final AsyncRetryExecutor<Object, Callable<? extends Object>> executor;
  private final Spf4jWebTarget target;
  private final long timeoutNanos;
  private final String method;

  public Spf4jInvocation(final Invocation invocation, final long timeoutNanos,
          final AsyncRetryExecutor<Object, Callable<? extends Object>> policy,
          final Spf4jWebTarget target, final String method) {
    this.invocation = invocation;
    this.executor = policy;
    this.timeoutNanos = timeoutNanos;
    this.target = target;
    this.method = method;
  }

  public String getMethod() {
    return method;
  }

  public long getTimeoutNanos() {
    return timeoutNanos;
  }

  public Spf4jWebTarget getTarget() {
    return target;
  }

  public String getName() {
    return method + ' ' + target.getUri();
  }

  @Override
  public Spf4jInvocation property(String name, Object value) {
    invocation.property(name, value);
    return this;
  }

  private <T> T invoke(Callable<T> what) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current, timeoutNanos, TimeUnit.NANOSECONDS);
    try (ExecutionContext ec = ExecutionContexts.start(getName(), current, nanoTime, deadlineNanos)) {
      return executor.call(what, RuntimeException.class, nanoTime, deadlineNanos);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    } catch (TimeoutException ex) {
      throw new UncheckedTimeoutException(ex);
    } catch (WebApplicationException ex) {
      throw handleServiceError(ex, current);
    }
  }

  @CheckReturnValue
  public WebApplicationException handleServiceError(final WebApplicationException ex,
          final ExecutionContext current) throws WebApplicationException {
    Response response = ex.getResponse();
    ServiceError se;
    try {
      se = response.readEntity(ServiceError.class);
    } catch (ProcessingException e) {
      // not a Propagable service error.
      ex.addSuppressed(e);
      return ex;
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
    return ex;
  }

  private <T> Future<T> submit(Callable<T> what) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current, timeoutNanos, TimeUnit.NANOSECONDS);
    Callable<T> pc = ExecutionContexts.propagatingCallable(what, current, getName(), deadlineNanos);
    return executor.submit(pc, nanoTime, deadlineNanos);
  }

  @Override
  public Response invoke() {
    return invoke(invocation::invoke);
  }

  @Override
  public <T> T invoke(Class<T> responseType) {
    return invoke(() -> invocation.invoke(responseType));
  }

  @Override
  public <T> T invoke(GenericType<T> responseType) {
    return invoke(() -> invocation.invoke(responseType));
  }

  @Override
  public Future<Response> submit() {
    return submit(invocation::invoke);
  }

  @Override
  public <T> Future<T> submit(Class<T> responseType) {
    return submit(() -> invocation.invoke(responseType));
  }

  @Override
  public <T> Future<T> submit(GenericType<T> responseType) {
    return submit(() -> invocation.invoke(responseType));
  }

  @Override
  public <T> Future<T> submit(InvocationCallback<T> callback) {
    final Type callbackParamType;
    final ReflectionHelper.DeclaringClassInterfacePair pair
            = ReflectionHelper.getClass(callback.getClass(), InvocationCallback.class);
    final Type[] typeArguments = ReflectionHelper.getParameterizedTypeArguments(pair);
    if (typeArguments == null || typeArguments.length == 0) {
      callbackParamType = Object.class;
    } else {
      callbackParamType = typeArguments[0];
    }
    final Class<T> callbackParamClass = ReflectionHelper.erasure(callbackParamType);
    if (Response.class == callbackParamClass) {
      return (Future<T>) submit(() -> {
        try {
          Response resp = invocation.invoke();
          callback.completed((T) resp);
          return resp;
        } catch (Throwable t) {
          callback.failed(t);
          throw t;
        }
      });
    } else {
      return (Future<T>) submit(() -> {
        try {
          T resp = invocation.invoke(new GenericType<>(callbackParamType));
          callback.completed(resp);
          return resp;
        } catch (Throwable t) {
          callback.failed(t);
          throw t;
        }
      });
    }
  }

  @Override
  public Invocation getWrapped() {
    return this.invocation;
  }

}
