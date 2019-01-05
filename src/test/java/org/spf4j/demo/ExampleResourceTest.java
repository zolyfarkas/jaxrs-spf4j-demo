/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.demo;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.demo.avro.DemoRecord;
import org.spf4j.demo.avro.DemoRecordInfo;
import org.spf4j.demo.avro.MetaData;
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

  @Test
  public void testPostRecordInfo() {
    List<DemoRecordInfo> records =
            Arrays.asList(
       DemoRecordInfo.newBuilder()
           .setDemoRecord(DemoRecord.newBuilder().setId("1")
           .setName("test").setDescription("testDescr").build())
           .setMetaData(MetaData.newBuilder()
                   .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                   .setLastModified(Instant.now())
                   .setLastAccessedBy("you").setLastModifiedBy("you").build()
           ).build());
    target.request()
            .post(Entity.entity(
                    new GenericEntity<>(records, new GenericType<List<DemoRecordInfo>>() {}.getType()),
                    MediaType.APPLICATION_JSON));
  }

  @Test
  public void testRestClientGet() {
    ExampleResource service = WebResourceFactory.newResource(ExampleResource.class, target);
    List<DemoRecordInfo> records = service.getRecords();
    LOG.debug("Received", records);
  }

  @Test
  public void testRestClientPost() {
    ExampleResource service = WebResourceFactory.newResource(ExampleResource.class, target);
    List<DemoRecordInfo> records =
            Arrays.asList(
       DemoRecordInfo.newBuilder()
           .setDemoRecord(DemoRecord.newBuilder().setId("1")
           .setName("test").setDescription("testDescr").build())
           .setMetaData(MetaData.newBuilder()
                   .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                   .setLastModified(Instant.now())
                   .setLastAccessedBy("you").setLastModifiedBy("you").build()
           ).build());
    service.saveRecords(records);
  }

  @Test
  public void testRestClientRoundTrip() {
    ExampleResource service = WebResourceFactory.newResource(ExampleResource.class, target);
    List<DemoRecordInfo> records = service.getRecords();
    LOG.debug("Received", records);
    service.saveRecords(records);
  }

}
