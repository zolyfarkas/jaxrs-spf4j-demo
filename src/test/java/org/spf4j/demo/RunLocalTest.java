
package org.spf4j.demo;

import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Assume;
import org.junit.Test;
import org.spf4j.test.log.TestUtils;

public class RunLocalTest {

  @Test(timeout = 1000000)
  public void run() throws IOException, InterruptedException, URISyntaxException {
    Assume.assumeTrue(TestUtils.isExecutedFromIDE());
    Main.main(new String[]{});
  }

}
