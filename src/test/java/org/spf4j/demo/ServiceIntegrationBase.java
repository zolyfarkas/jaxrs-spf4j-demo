
package org.spf4j.demo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.spf4j.grizzly.JerseyService;
import org.spf4j.grizzly.JvmServices;
import org.spf4j.grizzly.JvmServicesBuilder;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;

/**
 *
 * @author Zoltan Farkas
 */
public class ServiceIntegrationBase {

  static {
    System.setProperty("spf4j.failsafe.retryLogLevel", "DEBUG");
  }

  private static final JvmServices JVM = new JvmServicesBuilder()
            .withApplicationName("actuatorTest")
            .withLogFolder("./target")
            .withMetricsStore("WRAPPER@org.spf4j.demo.MetricsQueryRegister$Store(TSDB_AVRO@./target/test)")
            .build().start().closeOnShutdown();


  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static String localService;
  private static JerseyService svc;

  @BeforeClass
  public static void setUp() throws Exception {
    // start the server
    svc = Main.startServices(JVM, 9090, org.spf4j.base.Runtime.TMP_FOLDER);
    client = svc.getApplication().getRestClient();
    localService = "http://127.0.0.1:9090";
    target = client.target(localService);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (svc != null) {
      svc.close();
    }
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
