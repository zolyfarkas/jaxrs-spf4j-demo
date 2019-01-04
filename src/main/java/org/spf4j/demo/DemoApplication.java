
package org.spf4j.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.avro.SchemaResolver;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.spf4j.avro.SchemaClient;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.common.avro.AvroFeature;
import org.spf4j.jaxrs.server.Spf4jInterceptionService;

/**
 *
 * @author Zoltan Farkas
 */
@Singleton
public class DemoApplication extends ResourceConfig {

  private static volatile DemoApplication instance;

  private final SchemaClient schemaClient;

  private final Spf4JClient restClient;

  private final AppBinder appBinder;

  private final ServletContext srvContext;

  public DemoApplication(@Context ServletContext srvContext) {
    try {
      schemaClient = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    AvroFeature avroFeature = new AvroFeature(schemaClient);
    restClient = new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
//            .executorService(DefaultContextAwareExecutor.instance())
//            .scheduledExecutorService(DefaultContextAwareScheduledExecutor.instance())
            .readTimeout(60, TimeUnit.SECONDS)
            .register(ExecutionContextClientFilter.class)
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(avroFeature)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());
    appBinder = new AppBinder();
    register(appBinder);
    register(avroFeature);
    instance = this;
    this.srvContext = srvContext;
  }

  public void start() {
    ServletRegistration servletRegistration = srvContext.getServletRegistration("jersey");
    String uri = servletRegistration.getInitParameter("baseUri");
    for (String mapping : servletRegistration.getMappings()) {
      if (mapping.endsWith("/*")) {
        String path = mapping.substring(0, mapping.length() - 2);
        Response resp = restClient.target(uri).path(path).path("health/ping").request()
                .withTimeout(1, TimeUnit.SECONDS)
                .get();
        if (resp.getStatus() != 204) {
          throw new IllegalStateException("Application " + this + " failed to initialize, response  = " + resp);
        }
        Logger.getLogger(DemoApplication.class.getName())
                .info("Application initialized");
      }
    }
  }

  public static DemoApplication getInstance() {
    return instance;
  }

  public SchemaClient getSchemaClient() {
    return schemaClient;
  }

  public Spf4JClient getRestClient() {
    return restClient;
  }

  public AppBinder getAppBinder() {
    return appBinder;
  }

  public class AppBinder extends AbstractBinder {
      @Override
      protected void configure() {
          bind(schemaClient).to(SchemaResolver.class);
          bind(restClient).to(Client.class);
          bind(Spf4jInterceptionService.class)
                .to(org.glassfish.hk2.api.InterceptionService.class)
                .in(Singleton.class);
      }
  }

}
