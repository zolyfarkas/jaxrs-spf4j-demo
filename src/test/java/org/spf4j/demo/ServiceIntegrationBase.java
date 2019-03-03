
package org.spf4j.demo;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.stackmonitor.Sampler;

/**
 *
 * @author Zoltan Farkas
 */
public class ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceIntegrationBase.class);

  private static HttpServer server;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static Sampler profiler;
  private static String localService;

  @BeforeClass
  public static void setUp() throws Exception {
    // start the server
    profiler = Main.startProfiler();
    server = Main.startHttpServer();
    client = DemoApplication.getInstance().getRestClient();
    localService = "http://localhost:" + server.getListener("http").getPort();
    target = client.target(localService);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.shutdownNow();
    LOG.debug("Stack samples dumped to {}", profiler.dumpToFile());
    profiler.dispose();
  }

  public static HttpServer getServer() {
    return server;
  }

  public static Spf4jWebTarget getTarget() {
    return target;
  }

  public static Spf4JClient getClient() {
    return client;
  }

  public static Sampler getProfiler() {
    return profiler;
  }

  public static String getLocalService() {
    return localService;
  }


}
