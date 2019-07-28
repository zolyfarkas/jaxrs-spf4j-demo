
package org.spf4j.demo;

import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class FavIconTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(FavIconTest.class);


  @Test
  public void testGetDemoRecordInfo() {
    InputStream records =
            getTarget().path("favicon.ico")
                    .request(MediaType.WILDCARD).get(InputStream.class);
    LOG.debug("Received", records);
    Assert.assertNotNull(records);
  }

 @Test
  public void testIndex() {
    CharSequence records =
            getTarget().path("index.html")
                    .request(MediaType.WILDCARD).get(CharSequence.class);
    LOG.debug("Received", records);
    Assert.assertNotNull(records);
  }


  @Test
  public void testGetIndex() {
    CharSequence records =
            getTarget()
                    .queryParam("_log-level", "TRACE")
                    .request(MediaType.WILDCARD).get(CharSequence.class);
    LOG.debug("Received", records);
    Assert.assertNotNull(records);
  }

}
