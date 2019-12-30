package org.spf4j.demo;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
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
import javax.annotation.Nullable;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.apiBrowser.OpenApiResource;
import org.spf4j.actuator.cluster.health.DefaultClusterHealthChecksBinder;
import org.spf4j.actuator.health.checks.DefaultHealthChecksBinder;
import org.spf4j.avro.NoSnapshotRefsResolver;
import org.spf4j.avro.SchemaClient;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ThreadLocalContextAttacher;
import org.spf4j.base.avro.NetworkProtocol;
import org.spf4j.base.avro.NetworkService;
import org.spf4j.cluster.Cluster;
import org.spf4j.cluster.Service;
import org.spf4j.cluster.SingleNodeCluster;
import org.spf4j.hk2.Spf4jBinder;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.jaxrs.aql.AbacAuthorizer;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.common.providers.gp.CharSequenceMessageProvider;
import org.spf4j.jaxrs.common.providers.gp.CsvParameterConverterProvider;
import org.spf4j.jaxrs.common.providers.GZipEncoderDecoder;
import org.spf4j.jaxrs.common.providers.avro.AvroFeature;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.common.providers.gp.SampleNodeMessageProviderD3Json;
import org.spf4j.jaxrs.common.providers.gp.SampleNodeMessageProviderJson;
import org.spf4j.kube.client.Client;
import org.spf4j.kube.cluster.KubeCluster;
import org.spf4j.servlet.ExecutionContextFilter;
import org.spf4j.stackmonitor.ProfilingTLAttacher;
import org.spf4j.stackmonitor.Sampler;
import org.spf4j.stackmonitor.TracingExecutionContexSampler;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.spf4j.failsafe.HedgePolicy;

/**
 * @author Zoltan Farkas
 */
@Singleton
public class DemoApplication extends ResourceConfig {

  private static final Logger LOG = LoggerFactory.getLogger(DemoApplication.class);

  private static volatile DemoApplication instance;

  private final SchemaClient schemaClient;

  private final Spf4JClient restClient;

  private final ServletContext srvContext;

  private final Sampler sampler;

  @Inject
  public DemoApplication(@Context ServletContext srvContext, ServiceLocator locator) {
    ServiceLocatorUtilities.enableImmediateScope(locator);
    DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol();
    FilterRegistration ecFilter = srvContext.addFilter("server", new ExecutionContextFilter(dp));
    ecFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
    try {
      schemaClient = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    NoSnapshotRefsResolver noSnapshotRefsResolver = new NoSnapshotRefsResolver(schemaClient);
    SchemaResolvers.registerDefault(noSnapshotRefsResolver);
    AvroFeature avroFeature = new AvroFeature(
            new DefaultSchemaProtocol(noSnapshotRefsResolver),
            noSnapshotRefsResolver);
    restClient = Spf4JClient.create(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .register(new ExecutionContextClientFilter(dp, true))
            .register(ClientCustomExecutorServiceProvider.class)
            .register(ClientCustomScheduledExecutionServiceProvider.class)
            .register(new SampleNodeMessageProviderJson())
            .register(new SampleNodeMessageProviderD3Json())
            .register(new CsvParameterConverterProvider(Collections.EMPTY_LIST))
            .register(new CharSequenceMessageProvider())
            .register(GZipEncoderDecoder.class)
            .register(DeflateEncoder.class)
            .register(EncodingFilter.class)
            .register(avroFeature)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build()).withHedgePolicy(HedgePolicy.NONE);
    this.sampler = startProfiler();
      register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(sampler).to(Sampler.class);
        }
      });
    register(new Spf4jBinder(schemaClient, restClient, (x) -> true));
    register(avroFeature);
    register(GZipEncoderDecoder.class);
    registerClasses(OpenApiResource.class);
    String initParameter = srvContext.getServletRegistration("jersey").getInitParameter("servlet.port");
    register(new ClusterBinder(Integer.parseInt(initParameter)));
    register(new DefaultHealthChecksBinder());
    register(new DefaultClusterHealthChecksBinder());
    register(new AbstractBinder() {
      @Override
      protected void configure() {
        bind(AbacAuthorizer.ALL_ACCESS).to(AbacAuthorizer.class);
      }
    });
    if (instance != null) {
      throw new IllegalStateException("Application already initialized " + instance);
    }
    instance = this;
    this.srvContext = srvContext;
  }

  @Nullable
  public static Sampler startProfiler() {
    ThreadLocalContextAttacher threadLocalAttacher = ExecutionContexts.threadLocalAttacher();
    if (!(threadLocalAttacher instanceof ProfilingTLAttacher)) {
      LOG.warn("ProfilingTLAttacher is NOT active,"
              + " alternate profiling config already set up: {}", threadLocalAttacher);
      return null;
    }
    ProfilingTLAttacher contextFactory = (ProfilingTLAttacher) threadLocalAttacher;
    Sampler sampler = new Sampler(Integer.getInteger("app.profiler.sampleTimeMillis", 10),
            (t) -> new TracingExecutionContexSampler(contextFactory::getCurrentThreadContexts,
                    (ctx) -> {
                      String name = ctx.getName();
                      if (name.startsWith("GET")) {
                        return "GET";
                      } else if (ctx.getName().startsWith("POST")) {
                        return "POST";
                      } else {
                        return "OTHER";
                      }
                    }));
    sampler.registerJmx();
    sampler.start();
    return sampler;
  }

  public Sampler getSampler() {
    return sampler;
  }

  @PreDestroy
  public void cleanup() throws InterruptedException, IOException {
    if (sampler != null) {
      LOG.info("Stack samples dumped to {}", sampler.dumpToFile());
      sampler.dispose();
    }
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
    public ClusterBinder(@ConfigProperty(name = "servlet.port") final int port) {
      this.port = port;
    }

    @Override
    protected void configure() {
      String kubeNameSpace = System.getenv("KUBE_NAME_SPACE");
      if (kubeNameSpace == null) {
        ServletRegistration servletRegistration = srvContext.getServletRegistration("jersey");
        String bindAddr = servletRegistration.getInitParameter("servlet.bindAddr");
        try {
          SingleNodeCluster singleNodeCluster = new SingleNodeCluster(ImmutableSet.copyOf(InetAddress.getAllByName(bindAddr)),
                  Collections.singleton(new NetworkService("http",
                          port, NetworkProtocol.TCP)));
          bind(singleNodeCluster)
                  .to(Cluster.class);
          bind(singleNodeCluster).to(Service.class);
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
          KubeCluster kubeCluster = new KubeCluster(new Client(Suppliers.memoizeWithExpiration(
                  () -> {
            try {
              return Files.readString(Paths.get("/var/run/secrets/kubernetes.io/serviceaccount/token"),
                      StandardCharsets.UTF_8);
            } catch (IOException ex) {
              throw new UncheckedIOException(ex);
            }
          }, 10, TimeUnit.MINUTES), caCert), kubeNameSpace, System.getenv("KUBE_APP_NAME"));
          bind(kubeCluster).to(Cluster.class);
          bind(kubeCluster).to(Service.class);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
    }
  }

}
