
package org.spf4j.zhelloworld;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.apache.avro.SchemaResolver;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.spf4j.avro.SchemaClient;
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.concurrent.DefaultContextAwareScheduledExecutor;
import org.spf4j.jaxrs.client.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.common.CustomExecutorServiceProvider;
import org.spf4j.jaxrs.common.CustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.common.JsonAvroMessageBodyReader;
import org.spf4j.jaxrs.common.JsonAvroMessageBodyWriter;

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

  public DemoApplication() {
    try {
      schemaClient = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    restClient = new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .executorService(DefaultContextAwareExecutor.instance())
            .scheduledExecutorService(DefaultContextAwareScheduledExecutor.instance())
            .readTimeout(60, TimeUnit.SECONDS)
            .register(DemoApplication.getInstance().getAppBinder())
            .register(ExecutionContextClientFilter.class)
            .register(CustomExecutorServiceProvider.class)
            .register(CustomScheduledExecutionServiceProvider.class)
            .register(new JsonAvroMessageBodyReader(DemoApplication.getInstance().getClient()))
            .register(new JsonAvroMessageBodyWriter(DemoApplication.getInstance().getClient()))
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());
    appBinder = new AppBinder();
    register(appBinder);
    instance = this;
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
      }
  }

}
