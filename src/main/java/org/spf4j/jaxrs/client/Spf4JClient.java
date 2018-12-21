
package org.spf4j.jaxrs.client;

import java.net.URI;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author Zoltan Farkas
 */
public class Spf4JClient implements Client {

  private final Client cl;

  public Spf4JClient(final  Client cl) {
    this.cl = cl;
  }

  @Override
  public void close() {
    cl.close();
  }

  @Override
  public WebTarget target(String uri) {
    return cl.target(uri);
  }

  @Override
  public WebTarget target(URI uri) {
    return cl.target(uri);
  }

  @Override
  public WebTarget target(UriBuilder uriBuilder) {
    return cl.target(uriBuilder);
  }

  @Override
  public WebTarget target(Link link) {
    return cl.target(link);
  }

  @Override
  public Invocation.Builder invocation(Link link) {
    return cl.invocation(link);
  }

  @Override
  public SSLContext getSslContext() {
    return cl.getSslContext();
  }

  @Override
  public HostnameVerifier getHostnameVerifier() {
    return cl.getHostnameVerifier();
  }

  @Override
  public Configuration getConfiguration() {
    return cl.getConfiguration();
  }

  @Override
  public Client property(String name, Object value) {
    cl.property(name, value);
    return this;
  }

  @Override
  public Client register(Class<?> componentClass) {
    cl.register(componentClass);
    return this;
  }

  @Override
  public Client register(Class<?> componentClass, int priority) {
    cl.register(componentClass, priority);
    return this;
  }

  @Override
  public Client register(Class<?> componentClass, Class<?>... contracts) {
    cl.register(componentClass, contracts);
    return this;
  }

  @Override
  public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
    cl.register(componentClass, contracts);
    return this;
  }

  @Override
  public Client register(Object component) {
    cl.register(component);
    return this;
  }

  @Override
  public Client register(Object component, int priority) {
    cl.register(component, priority);
    return this;
  }

  @Override
  public Client register(Object component, Class<?>... contracts) {
    cl.register(component, contracts);
    return this;
  }

  @Override
  public Client register(Object component, Map<Class<?>, Integer> contracts) {
    cl.register(component, contracts);
    return this;
  }

}
