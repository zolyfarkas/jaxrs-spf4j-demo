package org.spf4j.demo;

import avro.shaded.com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import org.apache.avro.SchemaResolvers;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.spf4j.actuator.apiBrowser.OpenApiResource;
import org.spf4j.actuator.cluster.health.ClusterAllNodesCheck;
import org.spf4j.actuator.cluster.health.ClusterAllNodesRegistration;
import org.spf4j.actuator.health.HealthCheck;
import org.spf4j.avro.SchemaClient;
import org.spf4j.base.avro.NetworkProtocol;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.Service;
import org.spf4j.cluster.SingleNodeCluster;
import org.spf4j.hk2.Spf4jBinder;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.jaxrs.ConfigProperty;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.common.providers.CharSequenceMessageProvider;
import org.spf4j.jaxrs.common.providers.CsvParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.DirectStringMessageProvider;
import org.spf4j.jaxrs.common.providers.GZipEncoderDecoder;
import org.spf4j.jaxrs.common.providers.avro.AvroFeature;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.kube.client.Client;
import org.spf4j.kube.cluster.KubeCluster;
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

  @Inject
  public DemoApplication(@Context ServletContext srvContext, ServiceLocator locator) {
    ServiceLocatorUtilities.enableImmediateScope(locator);
    DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol();
    FilterRegistration testFilterReg = srvContext.addFilter("server", new ExecutionContextFilter(dp));
    testFilterReg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    try {
      schemaClient = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    SchemaResolvers.registerDefault(schemaClient);
    AvroFeature avroFeature = new AvroFeature(new DefaultSchemaProtocol(schemaClient), schemaClient);
    restClient = new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .register(new ExecutionContextClientFilter(dp, true))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(new CsvParameterConverterProvider(Collections.EMPTY_LIST))
            .register(new CharSequenceMessageProvider())
            .register(GZipEncoderDecoder.class)
            .register(DeflateEncoder.class)
            .register(EncodingFilter.class)
            .register(avroFeature)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());
    register(new Spf4jBinder(schemaClient, restClient, (x) -> true));
    register(avroFeature);
    register(CsvParameterConverterProvider.class);
    register(GZipEncoderDecoder.class);
    register(new DirectStringMessageProvider());
    register(new CharSequenceMessageProvider());
    registerClasses(OpenApiResource.class);
    String initParameter = srvContext.getServletRegistration("jersey").getInitParameter("servlet.port");
    register(new ClusterBinder(Integer.parseInt(initParameter)));
    register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(new HealthCheck.Registration() {
          @Override
          public String[] getPath() {
            return new String [] {"nop"};
          }

          @Override
          public HealthCheck getCheck() {
            return HealthCheck.NOP;
          }
        }).to(HealthCheck.Registration.class);
        bindAsContract(ClusterAllNodesCheck.class);
        bind(ClusterAllNodesRegistration.class).to(HealthCheck.Registration.class);
      }
    });
    if (instance != null) {
      throw new IllegalStateException("Application already initialized " + instance);
    }
    instance = this;
    this.srvContext = srvContext;
  }

  @PreDestroy
  public void cleanup() {
    instance = null;
  }

  public void start() {

// this used to start the ping on startup,
// but feature is not needed due to kebernetes readiness probe.
//    ServletRegistration servletRegistration = srvContext.getServletRegistration("jersey");
//    String uri = servletRegistration.getInitParameter("baseUri");
//    for (String mapping : servletRegistration.getMappings()) {
//      if (mapping.endsWith("/*")) {
//        String path = mapping.substring(0, mapping.length() - 2);
//        Response resp = restClient.target(uri).path(path).path("health/ping").request()
//                .withTimeout(1, TimeUnit.SECONDS)
//                .get();
//        if (resp.getStatus() != 204) {
//          throw new IllegalStateException("Application " + this + " failed to initialize, response  = " + resp);
//        }
//        Logger.getLogger(DemoApplication.class.getName())
//                .info("Application initialized");
//      }
//    }
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

  private class ClusterBinder extends AbstractBinder {

    private final int port;

    @Inject
    public ClusterBinder(@ConfigProperty("servlet.port") final int port) {
      this.port = port;
    }


    @Override
    protected void configure() {
      String kubeNameSpace = System.getenv("KUBE_NAME_SPACE");
      if (kubeNameSpace == null) {
        ServletRegistration servletRegistration = srvContext.getServletRegistration("jersey");
        String bindAddr = servletRegistration.getInitParameter("servlet.bindAddr");
        try {
          bind(new SingleNodeCluster(ImmutableSet.copyOf(InetAddress.getAllByName(bindAddr)),
                  Collections.singleton(new NetworkService("http",
                          port, NetworkProtocol.TCP))))
                  .to(Cluster.class);
        } catch (UnknownHostException ex) {
          throw new RuntimeException(ex);
        }
      } else {
        Path certPath = Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");
        byte[] caCert;
        try {
          if (Files.isReadable(certPath)) {
            caCert = Files.readAllBytes(certPath);
          } else {
            caCert = null;
          }
          KubeCluster kubeCluster = new KubeCluster(new Client(
                  Files.readString(
                          Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token"), StandardCharsets.UTF_8),
                  caCert),
                  kubeNameSpace, System.getenv("KUBE_APP_NAME"));
          bind(kubeCluster).to(Cluster.class);
          bind(kubeCluster).to(Service.class);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
    }
  }

}
