
package org.spf4j.demo;

import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.Ignore;
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
  @Ignore // not done yet
  public void testGetDemoRecordInfo() {
    InputStream records =
            getTarget().path("favicon.ico")
                    .request(MediaType.APPLICATION_JSON).get(InputStream.class);
    LOG.debug("Received", records);
    Assert.assertNotNull(records);
  }


}
