package org.spf4j.demo;


import javax.ws.rs.client.Invocation;


import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloConfigResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(HelloConfigResourceTest.class);

  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  public void testHello() {
    for (int i = 0; i < 100; i++) {
      Invocation.Builder request = getTarget().path("hello").request();
      String responseMsg = request.get(String.class);
      Assert.assertThat(responseMsg, Matchers.startsWith("Hi how are you?"));
    }
  }


}
