package org.spf4j.demo;

import io.swagger.v3.core.converter.ModelConverters;
import org.glassfish.grizzly.http.server.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.FixedWebappContext;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.apiBrowser.AvroModelConverter;
import org.spf4j.avro.SchemaClient;
import org.spf4j.base.Arrays;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.concurrent.LifoThreadPoolBuilder;
import org.spf4j.io.ByteArrayBuilder;
import org.spf4j.jaxrs.common.providers.avro.DefaultSchemaProtocol;
import org.spf4j.jaxrs.common.providers.avro.XJsonAvroMessageBodyWriter;
import org.spf4j.log.SLF4JBridgeHandler;
import org.spf4j.perf.ProcessVitals;
import org.spf4j.stackmonitor.ProfiledExecutionContextFactory;
import org.spf4j.stackmonitor.ProfilingTLAttacher;
import org.spf4j.stackmonitor.Sampler;
import org.spf4j.stackmonitor.TracingExecutionContexSampler;

/**
 * Main class.
 *
 */
public class Main {

  static {
    String appName = System.getenv("KUBE_APP_NAME");
    String podName = System.getenv("KUBE_POD_NAME");
    if (appName != null) {
      System.setProperty("appName", appName);
    }
    if (podName != null) {
       System.setProperty("logFileBase", podName);
       System.setProperty("podName", podName);
       System.setProperty("hostName", podName);
       System.setProperty("spf4j.perf.ms.defaultTsdbFolderPath", "/var/log");
       System.setProperty("spf4j.perf.ms.defaultSsdumpFolder", "/var/log");
    }
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    // Enable Continuous profiling.
    System.setProperty("spf4j.execContext.tlAttacherClass", ProfilingTLAttacher.class.getName());
    System.setProperty("spf4j.execContext.factoryClass", ProfiledExecutionContextFactory.class.getName());
  }

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);


  public static HttpServer startHttpServer() throws IOException, URISyntaxException {
    String envPort = System.getenv("APP_SERVICE_PORT");
    if (envPort == null) {
      return startHttpServer(System.getProperty("hostName", "127.0.0.1"), "0.0.0.0", 8080);
    } else {
      return startHttpServer(System.getProperty("hostName", "127.0.0.1"), "0.0.0.0", Integer.parseInt(envPort));
    }
  }

  public static HttpServer startHttpServer(final int port)
          throws IOException, URISyntaxException {
          return startHttpServer(System.getProperty("hostName", "127.0.0.1"), "0.0.0.0", port);
  }

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startHttpServer(final String hostName, final String bindAddr, final int port)
          throws IOException, URISyntaxException {
    FixedWebappContext webappContext = new FixedWebappContext("grizzly web context", "");
    ServletRegistration servletRegistration = webappContext.addServlet("jersey", ServletContainer.class);
    servletRegistration.addMapping("/demo/*");
    servletRegistration.setInitParameter("javax.ws.rs.Application", "org.spf4j.demo.DemoApplication");
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
            "org.spf4j.jaxrs.server.providers;org.spf4j.demo;org.spf4j.actuator");
//    servletRegistration.setInitParameter("jersey.config.server.tracing.type", "ALL");
    servletRegistration.setInitParameter("hostName", hostName);
    servletRegistration.setInitParameter("servlet.bindAddr", bindAddr);
    servletRegistration.setInitParameter("servlet.port", Integer.toString(port));
    servletRegistration.setInitParameter("servlet.protocol", "http");
    servletRegistration.setInitParameter("application.logFilesPath", "/var/log");
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setLoadOnStartup(0);
    HttpServer server = new HttpServer();
    server.getServerConfiguration()
            .setDefaultErrorPageGenerator(new ErrorPageGenerator() {
      @Override
      public String generate(Request request, int status, String reasonPhrase, String description, Throwable exception) {
        SchemaClient schemaClient = DemoApplication.getInstance().getSchemaClient();
        ServiceError err = ServiceError.newBuilder()
                .setCode(status)
                .setMessage(reasonPhrase + ';' +  description)
                .setDetail(new DebugDetail("origin", Collections.EMPTY_LIST,
                       exception != null ? Converters.convert(exception) : null, Collections.EMPTY_LIST))
                .build();
        ByteArrayBuilder bab = new ByteArrayBuilder(256);
        XJsonAvroMessageBodyWriter writer = new XJsonAvroMessageBodyWriter(new DefaultSchemaProtocol(schemaClient));
        try {
          writer.writeTo(err, err.getClass(), err.getClass(),
                  Arrays.EMPTY_ANNOT_ARRAY, MediaType.APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(2),
                  bab);
        } catch (RuntimeException ex) {
          if (exception != null) {
            ex.addSuppressed(exception);
          }
          LOG.error("Exception while writing detail", ex);
          throw ex;
        }  catch (IOException ex) {
          if (exception != null) {
            ex.addSuppressed(exception);
          }
          LOG.error("Exception while writing detail", ex);
          throw new UncheckedIOException(ex);
        }
        return bab.toString(StandardCharsets.UTF_8);
      }
    });
//  final ServerConfiguration config = server.getServerConfiguration();
//  config.addHttpHandler(new StaticHttpHandler(docRoot), "/");
    final NetworkListener listener
            = new NetworkListener("http", bindAddr, port);
    CompressionConfig compressionConfig = listener.getCompressionConfig();
    compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON); // the mode
    compressionConfig.setCompressionMinSize(4096); // the min amount of bytes to compress
    compressionConfig.setCompressibleMimeTypes("text/plain",
            "text/html", "text/csv", "application/json",
            "application/octet-stream", "application/avro",
            "application/avro+json", "application/avro-x+json"); // the mime types to compress
    TCPNIOTransport transport = listener.getTransport();
    transport.setKernelThreadPool(LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(Integer.getInteger("spf4j.grizzly.kernel.coreSize", 2))
            .withMaxSize(Integer.getInteger("spf4j.grizzly.kernel.maxSize", 8))
            .withDaemonThreads(true)
            .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.kernel.idleMillis", 120000))
            .withPoolName("gz-core")
            .withQueueSizeLimit(0)
            .enableJmx()
            .build());
    transport.setSelectorRunnersCount(Integer.getInteger("spf4j.grizzly.selectorCount", 4));
    transport.setWorkerThreadPool(LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(Integer.getInteger("spf4j.grizzly.worker.coreSize", 4))
            .withMaxSize(Integer.getInteger("spf4j.grizzly.worker.maxSize", 1024))
            .withDaemonThreads(false)
            .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.worker.idleMillis", 120000))
            .withPoolName("gz-work")
            .withQueueSizeLimit(0)
            .enableJmx()
            .build());
    server.addListener(listener);

    webappContext.deploy(server);
    server.start();
    DemoApplication.getInstance().start();
    return server;
  }

  public static Sampler startProfiler() {
    ProfilingTLAttacher contextFactory
            = (ProfilingTLAttacher) ExecutionContexts.threadLocalAttacher();
    Sampler sampler = new Sampler(1,
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

  /**
   * Main method.
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
    ModelConverters.getInstance().addConverter(AvroModelConverter.INSTANCE);
    ProcessVitals vitals = new ProcessVitals();
    vitals.start();
    final CountDownLatch latch = new CountDownLatch(1);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        vitals.close();
        latch.countDown();
      }

    });
    Sampler sampler = startProfiler();
    final HttpServer server = startHttpServer();
    LOG.info("Server started and listening at {}", server.getListeners());
    latch.await();
    server.shutdown(30, TimeUnit.SECONDS);
    server.shutdownNow();
    LOG.debug("Stack samples dumped to {}", sampler.dumpToFile());
    sampler.dispose();
  }

}
