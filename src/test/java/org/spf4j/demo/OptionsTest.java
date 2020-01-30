
package org.spf4j.demo;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    Response options = getTarget().request(MediaType.APPLICATION_XML_TYPE)
            .options();
    LOG.debug("Received", options.getAllowedMethods());
  }


}
