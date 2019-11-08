package org.spf4j.demo.resources;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.apache.avro.ArrayIterator;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.Projections;
import org.spf4j.demo.ExampleResource;
import org.spf4j.demo.avro.DemoRecord;
import org.spf4j.demo.avro.DemoRecordInfo;
import org.spf4j.demo.avro.MetaData;
import org.spf4j.base.CloseableIterable;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.log.ExecContextLogger;

/**
 * @author Zoltan Farkas
 */
@Path("example/records")
public class ExampleResourceImpl implements ExampleResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(ExampleResourceImpl.class));

  public List<DemoRecordInfo> getRecords() {
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
                            .setName("test").setDescription("testDescr").build())
                    .setMetaData(MetaData.newBuilder()
                            .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                            .setLastModified(Instant.now())
                            .setLastAccessedBy("you").setLastModifiedBy("you").build()
                    ).build());
  }

  public void saveRecords(List<DemoRecordInfo> records) {
    LOG.debug("Received", records);
  }

  @POST
  @Consumes({"text/csv"})
  @Path("stream")
  public void saveRecords(CloseableIterable<DemoRecordInfo> precords) throws IOException {
    try (CloseableIterable<DemoRecordInfo> records = precords) {
      for (DemoRecordInfo rec : records) {
        LOG.debug("Streamed", rec);
      }
    }
  }

  @Override
  public Iterable<GenericRecord> getRecordsProjection(Schema elementProjection) {
    return  IterableArrayContent.from(
            (List<GenericRecord>) Projections.project(Schema.createArray(elementProjection),
            Schema.createArray(DemoRecordInfo.getClassSchema()), getRecords()), elementProjection);
  }

}
