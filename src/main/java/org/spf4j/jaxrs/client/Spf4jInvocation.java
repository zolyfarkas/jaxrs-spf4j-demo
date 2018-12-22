package org.spf4j.jaxrs.client;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.failsafe.AsyncRetryExecutor;

/**
 * @author Zoltan Farkas
 */
public class Spf4jInvocation implements Invocation {

    private final Invocation invocation;
    private final AsyncRetryExecutor<Object, Callable<? extends Object>> executor;
    private final URI target;

    private Long timeoutMillis;

    public Spf4jInvocation(final Invocation invocation,
            final AsyncRetryExecutor<Object, Callable<? extends Object>> policy,
            @Nullable final Long timeoutMillis, final URI target) {
      this.invocation = invocation;
      this.executor = policy;
      this.timeoutMillis = timeoutMillis;
      this.target = target;
    }

    public Spf4jInvocation withTimeout(final long timeout, final TimeUnit tu) {
      timeoutMillis = tu.toMillis(timeout);
      return this;
    }

    @Override
    public Spf4jInvocation property(String name, Object value) {
      invocation.property(name, value);
      return this;
    }

    @Override
    public Response invoke() {
      try (ExecutionContext ec = timeoutMillis == null ? ExecutionContexts.start(target.toString())
              : ExecutionContexts.start(target.toString(), timeoutMillis, TimeUnit.MILLISECONDS)) {
          invocation.property(Spf4jClientProperties.EXEC_CONTEXT, ec);
          return executor.call(invocation::invoke, RuntimeException.class, timeoutMillis, TimeUnit.MILLISECONDS);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      } catch (TimeoutException ex) {
        throw new UncheckedTimeoutException(ex);
      }
    }

    @Override
    public <T> T invoke(Class<T> responseType) {
      try (ExecutionContext ec = timeoutMillis == null ? ExecutionContexts.start(target.toString())
              : ExecutionContexts.start(target.toString(), timeoutMillis, TimeUnit.MILLISECONDS)) {
        invocation.property(Spf4jClientProperties.EXEC_CONTEXT, ec);
        return executor.call(() -> invocation.invoke(responseType), RuntimeException.class);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      } catch (TimeoutException ex) {
        throw new UncheckedTimeoutException(ex);
      }
    }

    @Override
    public <T> T invoke(GenericType<T> responseType) {
      try (ExecutionContext ec = timeoutMillis == null ? ExecutionContexts.start(target.toString())
              : ExecutionContexts.start(target.toString(), timeoutMillis, TimeUnit.MILLISECONDS)) {
        invocation.property(Spf4jClientProperties.EXEC_CONTEXT, ec);
        return executor.call(() -> invocation.invoke(responseType), RuntimeException.class);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      } catch (TimeoutException ex) {
        throw new UncheckedTimeoutException(ex);
      }
    }

    @Override
    public Future<Response> submit() {
      return executor.submit(invocation::invoke);
    }

    @Override
    public <T> Future<T> submit(Class<T> responseType) {
      return executor.submit(() -> invocation.invoke(responseType));
    }

    @Override
    public <T> Future<T> submit(GenericType<T> responseType) {
      return executor.submit(() -> invocation.invoke(responseType));
    }

    @Override
    public <T> Future<T> submit(InvocationCallback<T> callback) {

      final Type callbackParamType;
      final ReflectionHelper.DeclaringClassInterfacePair pair =
                    ReflectionHelper.getClass(callback.getClass(), InvocationCallback.class);
      final Type[] typeArguments = ReflectionHelper.getParameterizedTypeArguments(pair);
      if (typeArguments == null || typeArguments.length == 0) {
        callbackParamType = Object.class;
      } else {
        callbackParamType = typeArguments[0];
      }
      final Class<T> callbackParamClass = ReflectionHelper.erasure(callbackParamType);

      if (Response.class == callbackParamClass) {
        return  executor.submit(() -> {
          try  {
            Response resp = invocation.invoke();
            callback.completed((T) resp);
            return resp;
          } catch (Throwable t) {
            callback.failed(t);
            throw t;
          }
        });
      } else {
        return  executor.submit(() -> {
          try  {
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
  }
