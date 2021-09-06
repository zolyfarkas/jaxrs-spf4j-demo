
package org.spf4j.demo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.spf4j.test.log.TestUtils;

@Ignore
public class RunLocalTest {

  @Test(timeout = 1000000)
  public void run() throws IOException, InterruptedException, URISyntaxException, ExecutionException, TimeoutException {
    Assume.assumeTrue(TestUtils.isExecutedFromIDE());
    Main.main(new String[]{});
    Thread.sleep(1000000);
  }

}
