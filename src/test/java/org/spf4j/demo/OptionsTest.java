
package org.spf4j.demo;

import com.google.common.net.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class OptionsTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(OptionsTest.class);

  @Test
  public void testGetDemoRecordInfo() {
    Response options = getTarget().request("application/vnd.sun.wadl+xml")
            .header(HttpHeaders.HOST, "www.google.com")
            .options();
    LOG.debug("Received", options.getAllowedMethods());
    Assert.assertTrue(options.getStatusInfo().getFamily() != Response.Status.Family.SERVER_ERROR);
  }


}
