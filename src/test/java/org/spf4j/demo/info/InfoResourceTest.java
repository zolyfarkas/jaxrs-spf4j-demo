
package org.spf4j.demo.info;

import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.ApplicationInfo;
import org.spf4j.base.avro.ClusterInfo;
import org.spf4j.demo.ServiceIntegrationBase;
import static org.spf4j.demo.ServiceIntegrationBase.getTarget;

/**
 * @author Zoltan Farkas
 */
public class InfoResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(InfoResourceTest.class);

  @Test
  public void testInfo() {
    ApplicationInfo ai = getTarget().path("info").request(MediaType.APPLICATION_JSON).get(ApplicationInfo.class);
    LOG.debug("application info", ai);
    Assert.assertNotNull(ai);
  }

  @Test(timeout = 10000)
  public void testClusterInfo() throws MalformedURLException {
    new URL("file://manifest/Implementation-Url");
    ClusterInfo ai = getTarget().path("info/cluster").request(MediaType.APPLICATION_JSON).get(ClusterInfo.class);
    LOG.debug("cluster info", ai);
    Assert.assertNotNull(ai);
  }

}
