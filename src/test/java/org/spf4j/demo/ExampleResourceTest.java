/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.demo;

import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.demo.avro.DemoRecordInfo;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;

/**
 *
 * @author Zoltan Farkas
 */
public class ExampleResourceTest {
  private static final Logger LOG = LoggerFactory.getLogger(ExampleResourceTest.class);

  private static HttpServer server;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;

  @BeforeClass
  public static void setUp() throws Exception {
    // start the server
    server = Main.startServer();
    client = DemoApplication.getInstance().getRestClient();
    target = client.target(Main.BASE_URI).path("example/records");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.shutdownNow();
  }

  @Test
  public void testGetDemoRecordInfo() {
    List<DemoRecordInfo> records =
            target.request(MediaType.APPLICATION_OCTET_STREAM_TYPE).get(new GenericType<List<DemoRecordInfo>>() {});
    LOG.debug("Received", records);
  }

}
