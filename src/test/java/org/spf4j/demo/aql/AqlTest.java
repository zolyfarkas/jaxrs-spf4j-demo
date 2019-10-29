
package org.spf4j.demo.aql;

import org.spf4j.demo.*;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CloseableIterable;

/**
 *
 * @author Zoltan Farkas
 */
public class AqlTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(AqlTest.class);


  @Test
  public void testGetPlanets() {
    try (CloseableIterable<Planet> planets =
            getTarget().path("avql/planets")
                    .request(MediaType.APPLICATION_JSON)
                    .get(new GenericType<CloseableIterable<Planet>>() {})) {
      int i = 0;
      for (Planet planet : planets) {
        LOG.debug("Received", planet);
        i++;
      }
      Assert.assertEquals(3, i);
    }
  }


}
