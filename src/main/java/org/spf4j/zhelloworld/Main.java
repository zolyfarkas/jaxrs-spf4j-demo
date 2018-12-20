package org.spf4j.zhelloworld;

import io.jaegertracing.Configuration;
import io.opentracing.util.GlobalTracer;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.spf4j.concurrent.LifoThreadPoolBuilder;
import org.spf4j.servlet.ExecutionContextFilter;

/**
 * Main class.
 *
 */
public class Main {
  // Base URI the Grizzly HTTP server will listen on

  public static final String BASE_URI = "http://0.0.0.0:8080/";

  /**
   * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Grizzly HTTP server.
   */
  public static HttpServer startServer() throws IOException {
    WebappContext webappContext = new WebappContext("grizzly web context", "");

    FilterRegistration testFilterReg = webappContext.addFilter("server", ExecutionContextFilter.class);
    testFilterReg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

    ServletRegistration servletRegistration = webappContext.addServlet("jersey", ServletContainer.class);
    servletRegistration.addMapping("/demo/*");
    servletRegistration.setInitParameter("jersey.config.server.provider.packages",
            "org.spf4j.zhelloworld;org.spf4j.jaxrs.server");
    HttpServer server = new HttpServer();
//  final ServerConfiguration config = server.getServerConfiguration();
//  config.addHttpHandler(new StaticHttpHandler(docRoot), "/");
    final NetworkListener listener
            = new NetworkListener("grizzly",
                    "0.0.0.0",
                    8080);
    listener.getTransport().setWorkerThreadPool(LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(Integer.getInteger("spf4j.grizzly.coreSize", 4))
            .withMaxSize(Integer.getInteger("spf4j.grizzly.auxMaxSize", 1024))
            .withDaemonThreads(false)
            .withMaxIdleTimeMillis(Integer.getInteger("spf4j.grizzly.idleMillis", 120000))
            .withPoolName("gz-http")
            .withQueueSizeLimit(0)
            .enableJmx()
            .build());
    server.addListener(listener);

    webappContext.deploy(server);
    server.start();
    return server;
  }

  private static final CountDownLatch latch = new CountDownLatch(1);

  /**
   * Main method.
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        latch.countDown();
      }

    });
    GlobalTracer.register(
            new Configuration(
                    "zhelloworld"
            ).getTracer());

    final HttpServer server = startServer();
    System.out.println(String.format("Jersey app started with WADL available at "
            + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
    latch.await();
    server.stop();
  }
}
