
package org.spf4j.jaxrs.client;

import java.util.concurrent.Future;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Zoltan Farkas
 */
public class Spf4jAsyncInvoker implements AsyncInvoker {

  private final Spf4jInvocationBuilder invocation;

  public Spf4jAsyncInvoker(Spf4jInvocationBuilder invocation) {
    this.invocation = invocation;
  }

  @Override
  public Future<Response> get() {
    return invocation.buildGet().submit();
  }

  @Override
  public <T> Future<T> get(Class<T> responseType) {
    return invocation.buildGet().submit(responseType);
  }

  @Override
  public <T> Future<T> get(GenericType<T> responseType) {
    return invocation.buildGet().submit(responseType);
  }

  @Override
  public <T> Future<T> get(InvocationCallback<T> callback) {
    return invocation.buildGet().submit(callback);
  }

  @Override
  public Future<Response> put(Entity<?> entity) {
    return invocation.buildPut(entity).submit();
  }

  @Override
  public <T> Future<T> put(Entity<?> entity, Class<T> responseType) {
    return invocation.buildPut(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> put(Entity<?> entity, GenericType<T> responseType) {
    return invocation.buildPut(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> put(Entity<?> entity, InvocationCallback<T> callback) {
    return invocation.buildPut(entity).submit(callback);
  }

  @Override
  public Future<Response> post(Entity<?> entity) {
    return invocation.buildPost(entity).submit();
  }

  @Override
  public <T> Future<T> post(Entity<?> entity, Class<T> responseType) {
    return invocation.buildPost(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> post(Entity<?> entity, GenericType<T> responseType) {
    return invocation.buildPost(entity).submit(responseType);
  }

  @Override
  public <T> Future<T> post(Entity<?> entity, InvocationCallback<T> callback) {
    return invocation.buildPost(entity).submit(callback);
  }

  @Override
  public Future<Response> delete() {
    return invocation.buildDelete().submit();
  }

  @Override
  public <T> Future<T> delete(Class<T> responseType) {
    return invocation.buildDelete().submit(responseType);
  }

  @Override
  public <T> Future<T> delete(GenericType<T> responseType) {
    return invocation.buildDelete().submit(responseType);
  }

  @Override
  public <T> Future<T> delete(InvocationCallback<T> callback) {
    return invocation.buildDelete().submit(callback);
  }

  @Override
  public Future<Response> head() {
    return invocation.build(HttpMethod.HEAD).submit();
  }

  @Override
  public Future<Response> head(InvocationCallback<Response> callback) {
    return invocation.build(HttpMethod.HEAD).submit(callback);
  }

  @Override
  public Future<Response> options() {
    return invocation.build(HttpMethod.OPTIONS).submit();
  }

  @Override
  public <T> Future<T> options(Class<T> responseType) {
    return invocation.build(HttpMethod.OPTIONS).submit(responseType);
  }

  @Override
  public <T> Future<T> options(GenericType<T> responseType) {
    return invocation.build(HttpMethod.OPTIONS).submit(responseType);
  }

  @Override
  public <T> Future<T> options(InvocationCallback<T> callback) {
     return invocation.build(HttpMethod.OPTIONS).submit(callback);
  }

  @Override
  public Future<Response> trace() {
    return invocation.build("TRACE").submit();
  }

  @Override
  public <T> Future<T> trace(Class<T> responseType) {
    return invocation.build("TRACE").submit(responseType);
  }

  @Override
  public <T> Future<T> trace(GenericType<T> responseType) {
    return invocation.build("TRACE").submit(responseType);
  }

  @Override
  public <T> Future<T> trace(InvocationCallback<T> callback) {
    return invocation.build("TRACE").submit(callback);
  }

  @Override
  public Future<Response> method(String name) {
    return invocation.build(name).submit();
  }

  @Override
  public <T> Future<T> method(String name, Class<T> responseType) {
    return invocation.build(name).submit(responseType);
  }

  @Override
  public <T> Future<T> method(String name, GenericType<T> responseType) {
    return invocation.build(name).submit(responseType);
  }

  @Override
  public <T> Future<T> method(String name, InvocationCallback<T> callback) {
    return invocation.build(name).submit(callback);
  }

  @Override
  public Future<Response> method(String name, Entity<?> entity) {
    return invocation.build(name, entity).submit();
  }

  @Override
  public <T> Future<T> method(String name, Entity<?> entity, Class<T> responseType) {
    return invocation.build(name, entity).submit(responseType);
  }

  @Override
  public <T> Future<T> method(String name, Entity<?> entity, GenericType<T> responseType) {
    return invocation.build(name, entity).submit(responseType);
  }

  @Override
  public <T> Future<T> method(String name, Entity<?> entity, InvocationCallback<T> callback) {
    return invocation.build(name, entity).submit(callback);
  }

}
