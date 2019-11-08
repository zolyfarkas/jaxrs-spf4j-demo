
package org.spf4j.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.apache.avro.reflect.AvroMeta;
import org.apache.avro.reflect.AvroSchema;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.demo.avro.DemoRecord;
import org.spf4j.demo.avro.DemoRecordInfo;
import org.spf4j.demo.avro.MetaData;
import org.apache.avro.reflect.LogicalType;
import org.spf4j.io.Streams;

/**
 *
 * @author Zoltan Farkas
 */
public class ExampleResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(ExampleResourceTest.class);


  @Test
  public void testGetDemoRecordInfo() {
    List<DemoRecordInfo> records =
            getTarget().path("example/records")
                    .request(MediaType.APPLICATION_JSON).get(new GenericType<List<DemoRecordInfo>>() {});
    LOG.debug("Received", records);
  }

  @Test
  public void testPostRecordInfo() {
    List<DemoRecordInfo> records =
            testRecords();
    getTarget().path("example/records").request()
            .post(Entity.entity(
                    new GenericEntity<>(records, new GenericType<List<DemoRecordInfo>>() {}.getType()),
                    MediaType.APPLICATION_JSON));
  }

  @Test
  public void testPostRecordInfoStream() {
    List<DemoRecordInfo> records =
            testRecords();
    Response post = getTarget().path("example/records/stream").request()
            .withTimeout(1000, TimeUnit.SECONDS)
            .post(Entity.entity(
                    new GenericEntity<>(records, new GenericType<List<DemoRecordInfo>>() {}.getType()),
                    "text/csv"));
    Assert.assertEquals(204, post.getStatus());
  }

  public static List<DemoRecordInfo> testRecords() {
    return Arrays.asList(
            DemoRecordInfo.newBuilder()
                    .setDemoRecord(DemoRecord.newBuilder().setId("1")
                            .setName("test").setDescription("testDescr").build())
                    .setMetaData(MetaData.newBuilder()
                            .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                            .setLastModified(Instant.now())
                            .setLastAccessedBy("you").setLastModifiedBy("you").build()
                    ).build(),
             DemoRecordInfo.newBuilder()
                    .setDemoRecord(DemoRecord.newBuilder().setId("2")
                            .setName("test2").setDescription("testDescr2").build())
                    .setMetaData(MetaData.newBuilder()
                            .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                            .setLastModified(Instant.now())
                            .setLastAccessedBy("you").setLastModifiedBy("you").build()
                    ).build());
  }

  @Test
  public void testRestClientGet() {
    ExampleResource service = WebResourceFactory.newResource(ExampleResource.class,
            getTarget().path("example/records"));
    List<DemoRecordInfo> records = service.getRecords();
    LOG.debug("Received", records);
  }

  @Test
  public void testRestClientGetCsv() throws IOException {
    InputStream records = getTarget().path("example/records").request("text/csv").get(InputStream.class);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Streams.copy(records, bos);
    LOG.debug("Received", new String(bos.toByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGetDemoRecordCsv() {
    List<DemoRecordInfo> records =
            getTarget().path("example/records").request("text/csv").get(new GenericType<List<DemoRecordInfo>>() {});
    LOG.debug("Received", records);
  }

  @Test
  public void testRestClientPost() {
    ExampleResource service = WebResourceFactory.newResource(ExampleResource.class, getTarget().path("example/records"));
    List<DemoRecordInfo> records =
            testRecords();
    service.saveRecords(records);
  }

  @Test
  public void testRestClientRoundTrip() {
    ExampleResource service = WebResourceFactory.newResource(ExampleResource.class, getTarget().path("example/records"));
    List<DemoRecordInfo> records = service.getRecords();
    LOG.debug("Received", records);
    service.saveRecords(records);
  }


  public static final class DemoProjection {
    @AvroMeta(key = "path", value = "$.demoRecord.id")
    private String id;

    @AvroMeta(key = "path", value = "$.metaData.lastAccessed")
    @AvroSchema("string")
    @LogicalType("instant")
    private Instant lastAccessed;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public Instant getLastAccessed() {
      return lastAccessed;
    }

    public void setLastAccessed(Instant lastAccessed) {
      this.lastAccessed = lastAccessed;
    }

    @Override
    public String toString() {
      return "DemoProjection{" + "id=" + id + ", lastAccessed=" + lastAccessed + '}';
    }

  }

  @Test
  public void testTypesProjection() {
    ExampleResourceExt service = WebResourceFactory.newResource(ExampleResourceExt.class,
            getTarget().path("example/records"));
    Iterable<DemoProjection> myInterest = service.getRecordsProjection(DemoProjection.class);
    LOG.debug("My  projection!", myInterest);
  }

}
