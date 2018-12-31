package org.spf4j.jaxrs.client;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.TimeSource;
import org.spf4j.failsafe.AsyncRetryExecutor;

/**
 * @author Zoltan Farkas
 */
public class Spf4jCompletionStageRxInvoker
        implements CompletionStageRxInvoker {

  private final Spf4jInvocationBuilder invocation;
  private final AsyncRetryExecutor<Object, Callable<? extends Object>> executor;

  public Spf4jCompletionStageRxInvoker(Spf4jInvocationBuilder invocation,
          AsyncRetryExecutor<Object, Callable<? extends Object>> executor) {
    this.invocation = invocation;
    this.executor = executor;
  }

  private String getName(final String method) {
    return method + ' ' + invocation.getTarget().getUri();
  }

  private <T> CompletableFuture<T> submit(Callable<T> what, final String name) {
    long nanoTime = TimeSource.nanoTime();
    ExecutionContext current = ExecutionContexts.current();
    long deadlineNanos = ExecutionContexts.computeDeadline(current, invocation.getTimeoutNanos(), TimeUnit.NANOSECONDS);
    Callable<T> pc = ExecutionContexts.propagatingCallable(what, current, name, deadlineNanos);
    return executor.submitRx(pc, nanoTime, deadlineNanos)
            .handle((result, ex) -> {
              if (ex != null) {
                Throwable rex = com.google.common.base.Throwables.getRootCause(ex);
                if (rex instanceof WebApplicationException) {
                  throw Utils.handleServiceError((WebApplicationException) rex, current);
                } else {
                  throw new RuntimeException(ex);
                }
              } else {
                return (T) result;
              }
            });
  }

  @Override
  public CompletionStage<Response> get() {
    return submit(invocation.buildGet().getWrapped()::invoke, getName(HttpMethod.GET));
  }

  @Override
  public <T> CompletionStage<T> get(Class<T> responseType) {
    return submit(() -> invocation.buildGet().getWrapped().invoke(responseType), getName(HttpMethod.GET));
  }

  @Override
  public <T> CompletionStage<T> get(GenericType<T> responseType) {
     return submit(() -> invocation.buildGet().getWrapped().invoke(responseType), getName(HttpMethod.GET));
  }

  @Override
  public CompletionStage<Response> put(Entity<?> entity) {
    return submit(() -> invocation.buildPut(entity).getWrapped().invoke(), getName(HttpMethod.PUT));
  }

  @Override
  public <T> CompletionStage<T> put(Entity<?> entity, Class<T> clazz) {
    return submit(() -> invocation.buildPut(entity).getWrapped().invoke(clazz), getName(HttpMethod.PUT));
  }

  @Override
  public <T> CompletionStage<T> put(Entity<?> entity, GenericType<T> type) {
    return submit(() -> invocation.buildPut(entity).getWrapped().invoke(type), getName(HttpMethod.PUT));
  }

  @Override
  public CompletionStage<Response> post(Entity<?> entity) {
    return submit(() -> invocation.buildPost(entity).getWrapped().invoke(), getName(HttpMethod.POST));
  }

  @Override
  public <T> CompletionStage<T> post(Entity<?> entity, Class<T> clazz) {
    return submit(() -> invocation.buildPost(entity).getWrapped().invoke(clazz), getName(HttpMethod.POST));
  }

  @Override
  public <T> CompletionStage<T> post(Entity<?> entity, GenericType<T> type) {
    return submit(() -> invocation.buildPost(entity).getWrapped().invoke(type), getName(HttpMethod.POST));
  }

  @Override
  public CompletionStage<Response> delete() {
    return submit(() -> invocation.buildDelete().getWrapped().invoke(), getName(HttpMethod.DELETE));
  }

  @Override
  public <T> CompletionStage<T> delete(Class<T> responseType) {
    return submit(() -> invocation.buildDelete().getWrapped().invoke(responseType), getName(HttpMethod.DELETE));
  }

  @Override
  public <T> CompletionStage<T> delete(GenericType<T> responseType) {
    return submit(() -> invocation.buildDelete().getWrapped().invoke(responseType), getName(HttpMethod.DELETE));
  }

  @Override
  public CompletionStage<Response> head() {
    return submit(() -> invocation.build(HttpMethod.HEAD).getWrapped().invoke(), getName(HttpMethod.HEAD));
  }

  @Override
  public CompletionStage<Response> options() {
    return submit(() -> invocation.build(HttpMethod.OPTIONS).getWrapped().invoke(), getName(HttpMethod.OPTIONS));
  }

  @Override
  public <T> CompletionStage<T> options(Class<T> responseType) {
    return submit(() -> invocation.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            getName(HttpMethod.OPTIONS));
  }

  @Override
  public <T> CompletionStage<T> options(GenericType<T> responseType) {
    return submit(() -> invocation.build(HttpMethod.OPTIONS).getWrapped().invoke(responseType),
            getName(HttpMethod.OPTIONS));
  }

  @Override
  public CompletionStage<Response> trace() {
    return submit(() -> invocation.build("TRACE").getWrapped().invoke(), getName("TRACE"));
  }

  @Override
  public <T> CompletionStage<T> trace(Class<T> responseType) {
    return submit(() -> invocation.build("TRACE").getWrapped().invoke(responseType), getName("TRACE"));
  }

  @Override
  public <T> CompletionStage<T> trace(GenericType<T> responseType) {
    return submit(() -> invocation.build("TRACE").getWrapped().invoke(responseType), getName("TRACE"));
  }

  @Override
  public CompletionStage<Response> method(String name) {
    return submit(() -> invocation.build(name).getWrapped().invoke(), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(String name, Class<T> responseType) {
    return submit(() -> invocation.build(name).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(String name, GenericType<T> responseType) {
    return submit(() -> invocation.build(name).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public CompletionStage<Response> method(String name, Entity<?> entity) {
     return submit(() -> invocation.build(name, entity).getWrapped().invoke(), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(String name, Entity<?> entity, Class<T> responseType) {
    return submit(() -> invocation.build(name, entity).getWrapped().invoke(responseType), getName(name));
  }

  @Override
  public <T> CompletionStage<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
    return submit(() -> invocation.build(name, entity).getWrapped().invoke(responseType), getName(name));
  }

}
