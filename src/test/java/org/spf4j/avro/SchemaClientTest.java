
package org.spf4j.avro;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.NotFoundException;
import org.apache.avro.Schema;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.spf4j.base.avro.DebugDetail;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaClientTest.class);

  @Test
  public void testSchemaClient() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    Schema schema = client.resolveSchema(DebugDetail.getClassSchema().getProp("mvnId"));
    Assert.assertEquals(DebugDetail.SCHEMA$.getName(), schema.getName());
  }

  @Test
  public void testArbitrarySchema() throws IOException, URISyntaxException {
    String mvnId = "org.spf4j.avro:core-schema:0.2:6";
    Files.deleteIfExists(Paths.get(org.spf4j.base.Runtime.USER_HOME,
            ".m2/repository/org/spf4j/avro/core-schema/0.2/core-schema-0.2.jar"));
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    Schema resolveSchema = client.resolveSchema(mvnId);
    Assert.assertEquals("ServiceError", resolveSchema.getName());
  }

  @Test
  public void testSchemaClient2() throws URISyntaxException {
    SchemaClient client = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
    try {
      client.resolveSchema("a:b:c:r");
      Assert.fail();
    } catch (NotFoundException ex) {
      LOG.debug("Expected exception", ex);
    }
    try {
      client.resolveSchema("a:b:c:r");
    } catch (NotFoundException ex) {
      Assert.assertThat(ex.getMessage(), Matchers.containsString("re-attempt"));
      LOG.debug("Expected exception", ex);
    }
  }

}
