
package org.spf4j.http;

import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class HttpWarningTest {

private static final Logger LOG = LoggerFactory.getLogger(HttpWarningTest.class);

  @Test
  public void testSomeMethod() {
    HttpWarning warning = new HttpWarning(HttpWarning.MISCELLANEOUS, "super", "aaaa   ", ZonedDateTime.now());
    LOG.debug("Warning", warning);
    HttpWarning nwarn = HttpWarning.parse(warning.toString());
    Assert.assertEquals(warning, nwarn);
  }

}
