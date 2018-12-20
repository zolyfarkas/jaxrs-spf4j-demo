package org.spf4j.jaxrs.client;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.failsafe.PartialTypedExceptionRetryPredicate;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.failsafe.concurrent.DefaultContextAwareRetryExecutor;

/**
 * @author Zoltan Farkas
 */
public final class FailsafeInvocations {


  private static final AsyncRetryExecutor<Object, Callable<? extends  Object>> DEFAULT_HTTP_RETRY_EXEC
          = RetryPolicy.newBuilder()
              .withDefaultThrowableRetryPredicate()
              .withExceptionPartialPredicate(WebApplicationException.class,
                      (WebApplicationException ex, c)  -> {
                          int status = ex.getResponse().getStatus();
                          switch (status) {
                            case 408:
                            case 409:
                              return
                          }
                          return null;
                      })
              .withRetryOnException(Exception.class, 2) // will retry any other exception twice.
              .build()
                  .async(HedgePolicy.DEFAULT, DefaultContextAwareRetryExecutor.instance());

  public static Invocation decorate(final Invocation invocation) {
    return decorate(invocation, DEFAULT_HTTP_RETRY_EXEC);
  }

  public static Invocation decorate(final Invocation invocation,
          final AsyncRetryExecutor<Object, Callable<? extends Object>> policy) {
    return new Invocation() {
      @Override
      public Invocation property(String name, Object value) {
        invocation.property(name, value);
        return this;
      }

      @Override
      public Response invoke() {
        try {
          return policy.call(invocation::invoke, RuntimeException.class);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        } catch (TimeoutException ex) {
          throw new UncheckedTimeoutException(ex);
        }
      }

      @Override
      public <T> T invoke(Class<T> responseType) {
        try {
          return policy.call(() -> invocation.invoke(responseType), RuntimeException.class);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        } catch (TimeoutException ex) {
          throw new UncheckedTimeoutException(ex);
        }
      }

      @Override
      public <T> T invoke(GenericType<T> responseType) {
        try {
          return policy.call(() -> invocation.invoke(responseType), RuntimeException.class);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        } catch (TimeoutException ex) {
          throw new UncheckedTimeoutException(ex);
        }
      }

      @Override
      public Future<Response> submit() {
          return policy.submit(invocation::invoke);
      }

      @Override
      public <T> Future<T> submit(Class<T> responseType) {
        return  policy.submit(() -> invocation.invoke(responseType));
      }

      @Override
      public <T> Future<T> submit(GenericType<T> responseType) {
        return  policy.submit(() -> invocation.invoke(responseType));
      }

      @Override
      public <T> Future<T> submit(InvocationCallback<T> callback) {
        return invocation.submit(callback);
      }

    };

  }

}
