package org.spf4j.demo;

import org.spf4j.grizzly.GrizzlyErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.FixedWebappContext;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.SchemaClient;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.ThreadLocalContextAttacher;
import org.spf4j.concurrent.LifoThreadPoolBuilder;
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

  private static final String LOG_FOLDER;

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
    }
    String logFolder = System.getenv("LOG_FOLDER");
    if (logFolder == null) {
      logFolder = "/var/log";
    }
    System.setProperty("logFolder", logFolder);
    LOG_FOLDER = logFolder;
    System.setProperty("spf4j.perf.ms.defaultTsdbFolderPath", logFolder);
    System.setProperty("spf4j.perf.ms.defaultSsdumpFolder", logFolder);
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
    servletRegistration.addMapping("/*");
    servletRegistration.setInitParameter("javax.ws.rs.Application", "org.spf4j.demo.DemoApplication");
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setInitParameter(ServerProperties.PROVIDER_SCANNING_RECURSIVE, "true");
    servletRegistration.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
              "org.spf4j.jaxrs.common.providers.gp;"
            + "org.spf4j.jaxrs.server.providers;"
                    + "org.spf4j.demo;"
                    + "org.spf4j.actuator;"
                    + "org.spf4j.actuator.cluster;"
                    + "org.spf4j.jaxrs.aql;"
                    + "org.spf4j.jaxrs.aql.server.providers");
//    servletRegistration.setInitParameter("jersey.config.server.tracing.type", "ALL");
    servletRegistration.setInitParameter("hostName", hostName);
    servletRegistration.setInitParameter("servlet.bindAddr", bindAddr);
    servletRegistration.setInitParameter("servlet.port", Integer.toString(port));
    servletRegistration.setInitParameter("servlet.protocol", "http");
    servletRegistration.setInitParameter("application.logFilesPath", LOG_FOLDER);
    servletRegistration.setLoadOnStartup(0);
    HttpServer server = new HttpServer();
    ServerConfiguration config = server.getServerConfiguration();
    config.setDefaultErrorPageGenerator(new GrizzlyErrorPageGenerator(
                    new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"))));
//    config.addHttpHandler(new CLStaticHttpHandler(Thread.currentThread().getContextClassLoader(), "/static/"),
//            "/*.ico", "/*.png");
    NetworkListener listener
            = createHttpListener(bindAddr, port);
    server.addListener(listener);

    webappContext.deploy(server);
    server.start();
    DemoApplication.getInstance().start();
    return server;
  }


  public static NetworkListener createHttpsListener(final String bindAddr, final int port) {
    //  final ServerConfiguration config = server.getServerConfiguration();
    NetworkListener listener = createHttpListener("https", bindAddr, port);
    listener.setSecure(true);
    return listener;
  }

//  private SSLEngineConfigurator createSSLConfig(boolean isServer)
//        throws Exception {
//    final SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
//    sslContextConfigurator.setKeyStoreFile(keyStoreFile);
//    // override system properties
//    final File cacerts = getStoreFile("server truststore",
//            "truststore_server.jks");
//    if (cacerts != null) {
//        sslContextConfigurator.setTrustStoreFile(cacerts.getAbsolutePath());
//        sslContextConfigurator.setTrustStorePass(TRUSTSTORE_PASSWORD);
//    }
//
//    // override system properties
//    final File keystore = getStoreFile("server keystore", "keystore_server.jks");
//    if (keystore != null) {
//        sslContextConfigurator.setKeyStoreFile(keystore.getAbsolutePath());
//        sslContextConfigurator.setKeyStorePass(TRUSTSTORE_PASSWORD);
//    }
//
//    //
//    boolean clientMode = false;
//    // force client Authentication ...
//    boolean needClientAuth = settings.isNeedClientAuth();
//    boolean wantClientAuth = settings.isWantClientAuth();
//    SSLEngineConfigurator result = new SSLEngineConfigurator(
//            sslContextConfigurator.createSSLContext(), clientMode, needClientAuth,
//            wantClientAuth);
//    return result;
//}

  public static NetworkListener createHttpListener(final String bindAddr,
          final int port) {
    return createHttpListener("http", bindAddr, port);
  }

  public static NetworkListener createHttpListener(final String name, final String bindAddr,
          final int port) {
    //  final ServerConfiguration config = server.getServerConfiguration();
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
    return listener;
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

  /**
   * Main method.
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
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
    if (sampler != null) {
      LOG.debug("Stack samples dumped to {}", sampler.dumpToFile());
      sampler.dispose();
    }
  }

}
