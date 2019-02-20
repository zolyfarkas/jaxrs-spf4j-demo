
package org.spf4j.demo.health;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * @author Zoltan Farkas
 */
public interface HealthCheck {

    public interface Registration {
      String[] getPath();

      HealthCheck getCheck();
    }

    void test(final Logger logger) throws Exception;

    String info();

    default long timeout(final TimeUnit tu) {
      return tu.convert(10, TimeUnit.SECONDS);
    }


}
