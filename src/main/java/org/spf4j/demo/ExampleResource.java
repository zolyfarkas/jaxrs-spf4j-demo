
package org.spf4j.demo;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.spf4j.demo.avro.DemoRecord;
import org.spf4j.demo.avro.DemoRecordInfo;
import org.spf4j.demo.avro.MetaData;

/**
 *
 * @author Zoltan Farkas
 */
@Path("example/records")
public class ExampleResource {

 @GET
 public List<DemoRecordInfo>  getRecords() {
   return Arrays.asList(
       DemoRecordInfo.newBuilder()
           .setDemoRecord(DemoRecord.newBuilder().setId("1")
           .setName("test").setDescription("testDescr").build())
           .setMetaData(MetaData.newBuilder()
                   .setAsOf(Instant.now()).setLastAccessed(Instant.now())
                   .setLastModified(Instant.now())
                   .setLastAccessedBy("you").setLastModifiedBy("you").build()
           ).build());
 }

}
