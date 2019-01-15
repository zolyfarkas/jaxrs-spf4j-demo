package org.spf4j.demo;

import org.glassfish.grizzly.http.server.HttpServer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.servlet.FixedWebappContext;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.concurrent.LifoThreadPoolBuilder;
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
    // Enable Continuous profiling.
    System.setProperty("spf4j.execContext.tlAttacherClass", ProfilingTLAttacher.class.getName());
    System.setProperty("spf4j.execContext.factoryClass", ProfiledExecutionContextFactory.class.getName());
  }

  // Base URI the Grizzly HTTP server will listen on
  public static final String BASE_URI = "http://0.0.0.0:8080/";

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startHttpServer() throws IOException, URISyntaxException {
    FixedWebappContext webappContext = new FixedWebappContext("grizzly web context", "");
    ServletRegistration servletRegistration = webappContext.addServlet("jersey", ServletContainer.class);
    servletRegistration.addMapping("/demo/*");
    servletRegistration.setInitParameter("javax.ws.rs.Application", "org.spf4j.demo.DemoApplication");
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setInitParameter(ServerProperties.PROVIDER_PACKAGES,
            "org.spf4j.jaxrs.server;org.spf4j.demo");
//    servletRegistration.setInitParameter("jersey.config.server.tracing.type", "ALL");
    servletRegistration.setInitParameter("baseUri", BASE_URI);
    servletRegistration.setInitParameter(ServerProperties.PROCESSING_RESPONSE_ERRORS_ENABLED, "true");
    servletRegistration.setLoadOnStartup(0);
    URI srvUri = new URI(BASE_URI);
    HttpServer server = new HttpServer();
//  final ServerConfiguration config = server.getServerConfiguration();
//  config.addHttpHandler(new StaticHttpHandler(docRoot), "/");
    final NetworkListener listener
            = new NetworkListener("grizzly",
                    srvUri.getHost(),
                    srvUri.getPort());
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
    final CountDownLatch latch = new CountDownLatch(1);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        latch.countDown();
      }

    });
    Sampler sampler
            = startProfiler();
    final HttpServer server = startHttpServer();
    System.out.println(String.format("Jersey app started with WADL available at "
            + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
    latch.await();
    server.shutdown(30, TimeUnit.SECONDS);
    server.shutdownNow();
    sampler.dispose();
  }

}
