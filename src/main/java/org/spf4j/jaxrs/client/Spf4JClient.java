
package org.spf4j.jaxrs.client;

import org.spf4j.jaxrs.Utils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.spf4j.failsafe.AsyncRetryExecutor;
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.failsafe.RetryPolicy;
import org.spf4j.failsafe.concurrent.DefaultFailSafeExecutor;
import org.spf4j.failsafe.concurrent.FailSafeExecutor;

/**
 *
 * @author Zoltan Farkas
 */
public class Spf4JClient implements Client {

  static {
    // time to cache DNS entries in seconds.
    System.setProperty("networkaddress.cache.ttl", "20");
    // time to cache failed attempts in seconds.
    System.setProperty("networkaddress.cache.negative.ttl", "5");
  }

  private final Client cl;

  private final RetryPolicy retryPolicy;

  private final HedgePolicy hedgePolicy;

  private final FailSafeExecutor fsExec;

  private final AsyncRetryExecutor<Object, Callable<? extends Object>> executor;

  public Spf4JClient(final  Client cl) {
    this(cl, Utils.defaultRetryPolicy(), HedgePolicy.DEFAULT, DefaultFailSafeExecutor.instance());
  }


  public Spf4JClient(final  Client cl, final RetryPolicy retryPolicy, final HedgePolicy hedgePolicy,
          final FailSafeExecutor fsExec) {
    this.cl = cl;
    ClientConfig configuration = (ClientConfig) cl.getConfiguration();
    HttpUrlConnectorProvider httpUrlConnectorProvider = new HttpUrlConnectorProvider();
    httpUrlConnectorProvider.connectionFactory(new HttpUrlConnectorProvider.ConnectionFactory() {

      /**
       * Attempt to client side load balance...
       * Approach works for HTTP only.
       * for HTTPS  we would need to register a new HTTP URL connection.
       *
       * @param url
       * @return
       * @throws IOException
       */
      @Override
      public HttpURLConnection getConnection(final URL url) throws IOException {
        try {
          if (url.getProtocol().equals("https")) {
            return (HttpURLConnection) url.openConnection();
          }
          URI uri = url.toURI();
          InetAddress[] targets = InetAddress.getAllByName(uri.getHost());
          InetAddress chosen = targets[ThreadLocalRandom.current().nextInt(targets.length)];
          URI newUri = new URI(uri.getScheme(), uri.getUserInfo(),
                  chosen.getHostAddress(), uri.getPort(), uri.getPath(),
                  uri.getQuery(), uri.getFragment());
          return (HttpURLConnection) newUri.toURL().openConnection();
        } catch (URISyntaxException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    configuration.connectorProvider(httpUrlConnectorProvider);
    this.retryPolicy = retryPolicy;
    this.hedgePolicy = hedgePolicy;
    this.fsExec = fsExec;
    this.executor = retryPolicy.async(hedgePolicy, fsExec);
  }


  public Spf4JClient withHedgePolicy(final HedgePolicy hp) {
    return new Spf4JClient(cl, retryPolicy, hp, fsExec);
  }

  public Spf4JClient withRetryPolicy(final RetryPolicy rp) {
    return new Spf4JClient(cl, rp, hedgePolicy, fsExec);
  }

  @Override
  public void close() {
    cl.close();
  }

  @Override
  public Spf4jWebTarget target(String uri) {
    return new Spf4jWebTarget(this, cl.target(uri), executor, null);
  }

  @Override
  public Spf4jWebTarget target(URI uri) {
    return new Spf4jWebTarget(this, cl.target(uri), executor, null);
  }

  @Override
  public Spf4jWebTarget target(UriBuilder uriBuilder) {
    return new Spf4jWebTarget(this, cl.target(uriBuilder), executor, null);
  }

  @Override
  public Spf4jWebTarget target(Link link) {
    return new Spf4jWebTarget(this, cl.target(link), executor, null);
  }

  @Override
  public Spf4jInvocationBuilder invocation(Link link) {
    return new Spf4jInvocationBuilder(this, cl.invocation(link), executor, new Spf4jWebTarget(this, cl.target(link),
            executor, null));
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
  public Spf4JClient property(String name, Object value) {
    cl.property(name, value);
    return this;
  }

  @Override
  public Spf4JClient register(Class<?> componentClass) {
    cl.register(componentClass);
    return this;
  }

  @Override
  public Spf4JClient register(Class<?> componentClass, int priority) {
    cl.register(componentClass, priority);
    return this;
  }

  @Override
  public Spf4JClient register(Class<?> componentClass, Class<?>... contracts) {
    cl.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4JClient register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
    cl.register(componentClass, contracts);
    return this;
  }

  @Override
  public Spf4JClient register(Object component) {
    cl.register(component);
    return this;
  }

  @Override
  public Spf4JClient register(Object component, int priority) {
    cl.register(component, priority);
    return this;
  }

  @Override
  public Spf4JClient register(Object component, Class<?>... contracts) {
    cl.register(component, contracts);
    return this;
  }

  @Override
  public Spf4JClient register(Object component, Map<Class<?>, Integer> contracts) {
    cl.register(component, contracts);
    return this;
  }

}
