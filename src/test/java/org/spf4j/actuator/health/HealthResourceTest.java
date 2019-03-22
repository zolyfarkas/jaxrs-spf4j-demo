
package org.spf4j.actuator.health;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.MediaType;
import org.apache.avro.AvroUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.HealthCheckInfo;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.demo.ServiceIntegrationBase;
import static org.spf4j.demo.ServiceIntegrationBase.getTarget;
import org.spf4j.io.Streams;

/**
 * @author Zoltan Farkas
 */
public class HealthResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(HealthResourceTest.class);

  @Test
  public void testHealthInfo() {
    HealthCheckInfo ai = getTarget().path("health/info")
            .request(MediaType.APPLICATION_JSON).get(HealthCheckInfo.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheck() {
    HealthRecord ai = getTarget().path("health/check")
            .request(MediaType.APPLICATION_JSON).get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheckLocal() {
    HealthRecord ai = getTarget().path("health/check/local")
            .request(MediaType.APPLICATION_JSON).get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheckCluster() {
    HealthRecord ai = getTarget().path("health/check/cluster")
            .request("application/avro").get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }


  @Test
  public void testHealthCheckCluster2() {
    HealthRecord ai = getTarget().path("health/check/cluster")
            .queryParam("debug", "true")
            .request("application/json").get(HealthRecord.class);
    LOG.debug("health checks info", ai);
    Assert.assertNotNull(ai);
  }

  @Test
  public void testHealthCheckCluster2Capture() throws IOException {
    InputStream ai = getTarget().path("health/check/cluster")
            .queryParam("debug", "true")
            .request("application/json").get(InputStream.class);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Streams.copy(ai, bos);
    System.out.println(new String(bos.toByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testHealthCheckCluster2Repro() throws IOException {
    AvroUtils.readAvroExtendedJson(ClassLoader.getSystemResourceAsStream("newjson.json"), HealthRecord.class);
  }


}
