
package org.spf4j.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.SchemaResolvers;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.spf4j.avro.SchemaClient;
import org.spf4j.hk2.Spf4jBinder;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.common.CsvParameterConverterProvider;
import org.spf4j.jaxrs.common.avro.AvroFeature;
import org.spf4j.jaxrs.common.avro.DefaultSchemaProtocol;
import org.spf4j.servlet.ExecutionContextFilter;

/**
 * @author Zoltan Farkas
 */
@Singleton
public class DemoApplication extends ResourceConfig {

  private static volatile DemoApplication instance;

  private final SchemaClient schemaClient;

  private final Spf4JClient restClient;

  private final ServletContext srvContext;

  public DemoApplication(@Context ServletContext srvContext) {
    DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol();
    FilterRegistration testFilterReg = srvContext.addFilter("server", new ExecutionContextFilter(dp));
    testFilterReg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    try {
      schemaClient = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    SchemaResolvers.registerDefault(new AvroNamesRefResolver(schemaClient));
    AvroFeature avroFeature = new AvroFeature(new DefaultSchemaProtocol(schemaClient), schemaClient);
    restClient = new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .register(new ExecutionContextClientFilter(dp))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(new CsvParameterConverterProvider(Collections.EMPTY_LIST))
            .register(avroFeature)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());
    register(new Spf4jBinder(schemaClient, restClient, (x) -> true));
    register(avroFeature);
    register(CsvParameterConverterProvider.class);
    if (instance != null) {
      throw new IllegalStateException("Application already initialized " + instance);
    }
    instance = this;
    this.srvContext = srvContext;
  }

  @PreDestroy
  public void cleanup () {
    instance = null;
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

}
