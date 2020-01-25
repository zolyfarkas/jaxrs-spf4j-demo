
package org.spf4j.demo;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;

/**
 *
 * @author Zoltan Farkas
 */
public class ServiceIntegrationBase {

  private static HttpServer server;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static String localService;

  @BeforeClass
  public static void setUp() throws Exception {
    // start the server
    server = Main.startHttpServer("127.0.0.1", "127.0.0.1", 8080);
    DemoApplication app = DemoApplication.getInstance();
    client = app.getRestClient();
    localService = "http://127.0.0.1:" + server.getListener("http").getPort();
    target = client.target(localService);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.shutdownNow();
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


  public static String getLocalService() {
    return localService;
  }


}
