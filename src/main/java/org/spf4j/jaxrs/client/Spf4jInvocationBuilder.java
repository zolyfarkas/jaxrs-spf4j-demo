
package org.spf4j.jaxrs.client;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.CompletionStageRxInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.RxInvoker;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.spf4j.failsafe.AsyncRetryExecutor;

/**
 * @author Zoltan Farkas
 */
public class Spf4jInvocationBuilder implements Invocation.Builder {

  private final Invocation.Builder ib;
  private final Spf4jWebTarget target;
  private AsyncRetryExecutor<Object, Callable<? extends Object>> executor;
  private long timeoutNanos;

  public Spf4jInvocationBuilder(final Spf4JClient client, final Invocation.Builder ib,
          final AsyncRetryExecutor<Object, Callable<? extends Object>> executor,
          final Spf4jWebTarget target) {
    this.ib = ib;
    this.executor = executor;
    this.target = target;
    Number timeout =  (Number) client.getConfiguration().getProperty(Spf4jClientProperties.TIMEOUT_NANOS);
    if (timeout != null) {
      this.timeoutNanos = timeout.longValue();
    } else {
      this.timeoutNanos = Long.getLong(Spf4jClientProperties.TIMEOUT_NANOS, 60000000000L);
    }
  }

  public Spf4jWebTarget getTarget() {
    return target;
  }

  public long getTimeoutNanos() {
    return timeoutNanos;
  }

  public Spf4jInvocationBuilder withRetryRexecutor(final AsyncRetryExecutor<Object, Callable<? extends Object>> exec) {
    this.executor = exec;
    return this;
  }

  public Spf4jInvocationBuilder withTimeout(final long timeout, final TimeUnit tu) {
    timeoutNanos = tu.toNanos(timeout);
    return this;
  }

  @Override
  public Spf4jInvocation build(final String method) {
    return new Spf4jInvocation(ib.build(method), timeoutNanos, executor, this.target, method);
  }

  @Override
  public Spf4jInvocation build(final String method, final Entity<?> entity) {
    return new  Spf4jInvocation(ib.build(method, entity), timeoutNanos, executor, this.target, method);
  }

  @Override
  public Spf4jInvocation buildGet() {
    return new Spf4jInvocation(ib.buildGet(), timeoutNanos, executor, this.target, HttpMethod.GET);
  }

  @Override
  public Spf4jInvocation buildDelete() {
    return new Spf4jInvocation(ib.buildDelete(), timeoutNanos, executor, this.target, HttpMethod.DELETE);
  }

  @Override
  public Spf4jInvocation buildPost(Entity<?> entity) {
    return new Spf4jInvocation(ib.buildPost(entity), timeoutNanos, executor, this.target, HttpMethod.POST);
  }

  @Override
  public Spf4jInvocation buildPut(Entity<?> entity) {
    return new Spf4jInvocation(ib.buildPut(entity), timeoutNanos, executor, this.target, HttpMethod.PUT);
  }

  @Override
  public Spf4jAsyncInvoker async() {
    return new Spf4jAsyncInvoker(this);
  }

  @Override
  public Invocation.Builder accept(String... mediaTypes) {
    ib.accept(mediaTypes);
    return this;
  }

  @Override
  public Invocation.Builder accept(MediaType... mediaTypes) {
    ib.accept(mediaTypes);
    return this;
  }

  @Override
  public Invocation.Builder acceptLanguage(Locale... locales) {
    ib.acceptLanguage(locales);
    return this;
  }

  @Override
  public Invocation.Builder acceptLanguage(String... locales) {
    ib.acceptLanguage(locales);
    return this;
  }

  @Override
  public Invocation.Builder acceptEncoding(String... encodings) {
    ib.acceptEncoding(encodings);
    return this;
  }

  @Override
  public Invocation.Builder cookie(Cookie cookie) {
    ib.cookie(cookie);
    return this;
  }

  @Override
  public Invocation.Builder cookie(String name, String value) {
    ib.cookie(name, value);
    return this;
  }

  @Override
  public Invocation.Builder cacheControl(CacheControl cacheControl) {
    ib.cacheControl(cacheControl);
    return this;
  }

  @Override
  public Invocation.Builder header(String name, Object value) {
    ib.header(name, value);
    return this;
  }

  @Override
  public Invocation.Builder headers(MultivaluedMap<String, Object> headers) {
    ib.headers(headers);
    return this;
  }

  @Override
  public Spf4jInvocationBuilder property(String name, Object value) {
    ib.property(name, value);
    return this;
  }

  @Override
  public CompletionStageRxInvoker rx() {
    return new Spf4jCompletionStageRxInvoker(this, executor);
  }

  @Override
  public <T extends RxInvoker> T rx(Class<T> clazz) {
    return ib.rx(clazz);
  }

  @Override
  public Response get() {
    return buildGet().invoke();
  }

  @Override
  public <T> T get(Class<T> responseType) {
    return buildGet().invoke(responseType);
  }

  @Override
  public <T> T get(GenericType<T> responseType) {
    return buildGet().invoke(responseType);
  }

  @Override
  public Response put(Entity<?> entity) {
     return buildPut(entity).invoke();
  }

  @Override
  public <T> T put(Entity<?> entity, Class<T> responseType) {
    return buildPut(entity).invoke(responseType);
  }

  @Override
  public <T> T put(Entity<?> entity, GenericType<T> responseType) {
    return buildPut(entity).invoke(responseType);
  }

  @Override
  public Response post(Entity<?> entity) {
    return buildPost(entity).invoke();
  }

  @Override
  public <T> T post(Entity<?> entity, Class<T> responseType) {
    return buildPost(entity).invoke(responseType);
  }

  @Override
  public <T> T post(Entity<?> entity, GenericType<T> responseType) {
    return buildPost(entity).invoke(responseType);
  }

  @Override
  public Response delete() {
    return buildDelete().invoke();
  }

  @Override
  public <T> T delete(Class<T> responseType) {
    return buildDelete().invoke(responseType);
  }

  @Override
  public <T> T delete(GenericType<T> responseType) {
    return buildDelete().invoke(responseType);
  }

  @Override
  public Response head() {
    return build(HttpMethod.HEAD).invoke();
  }

  @Override
  public Response options() {
    return build(HttpMethod.OPTIONS).invoke();
  }

  @Override
  public <T> T options(Class<T> responseType) {
    return build(HttpMethod.OPTIONS).invoke(responseType);
  }

  @Override
  public <T> T options(GenericType<T> responseType) {
    return build(HttpMethod.OPTIONS).invoke(responseType);
  }

  @Override
  public Response trace() {
    return build("TRACE").invoke();
  }

  @Override
  public <T> T trace(Class<T> responseType) {
    return build("TRACE").invoke(responseType);
  }

  @Override
  public <T> T trace(GenericType<T> responseType) {
    return build("TRACE").invoke(responseType);
  }

  @Override
  public Response method(String name) {
    return build(name).invoke();
  }

  @Override
  public <T> T method(String name, Class<T> responseType) {
    return build(name).invoke(responseType);
  }

  @Override
  public <T> T method(String name, GenericType<T> responseType) {
    return build(name).invoke(responseType);
  }

  @Override
  public Response method(String name, Entity<?> entity) {
    return build(name, entity).invoke();
  }

  @Override
  public <T> T method(String name, Entity<?> entity, Class<T> responseType) {
    return build(name, entity).invoke(responseType);
  }

  @Override
  public <T> T method(String name, Entity<?> entity, GenericType<T> responseType) {
    return build(name, entity).invoke(responseType);
  }

}
