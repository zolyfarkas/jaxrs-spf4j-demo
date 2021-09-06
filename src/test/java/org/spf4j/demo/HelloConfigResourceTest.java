package org.spf4j.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.ws.rs.client.Invocation;

import org.hamcrest.Matchers;
import org.junit.After;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloConfigResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(HelloConfigResourceTest.class);

  private static final Path config = Paths.get(System.getenv("APP_CONFIG_MAP_DIR"));

  @Before
  public void setup() throws IOException {
    reset();
  }

  @After
  public void after() throws IOException {
    reset();
  }

  public void reset() throws IOException {
    Files.writeString(config.resolve("hello.feature"), "true", StandardCharsets.UTF_8);
    Files.writeString(config.resolve("demo.config"),
            "#Content-Type:application/json;avsc="
            + "\"\\{\\\"$ref\\\":\\\"org.spf4j.demo:jaxrs-spf4j-demo-schema:0.10:c\\\"\\}\"\n"
            + "{\n"
            + "  \"strVal\": \"Test Value 1\"\n"
            + "}\n"
            + "", StandardCharsets.UTF_8);
  }

  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  public void testHello() throws IOException, InterruptedException {
    for (int i = 0; i < 100; i++) {
      Invocation.Builder request = getTarget().path("hello").request();
      String responseMsg = request.get(String.class);
      Assert.assertThat(responseMsg, Matchers.startsWith("Hi how are you?"));
    }
    Files.writeString(config.resolve("hello.feature"), "false", StandardCharsets.UTF_8);
    Files.writeString(config.resolve("demo.config"),
            "#Content-Type:application/json;avsc="
            + "\"\\{\\\"$ref\\\":\\\"org.spf4j.demo:jaxrs-spf4j-demo-schema:0.10:c\\\"\\}\"\n"
            + "{\n"
            + "  \"strVal\": \"cucu banana\"\n"
            + "}\n"
            + "", StandardCharsets.UTF_8);
    for (int i = 0; i < 30; i++) {
      Invocation.Builder request = getTarget().path("hello").request();
      String responseMsg = request.get(String.class);
      LOG.debug("Returned: {}", responseMsg);
      if (responseMsg.contains("cucu banana")) {
        return;
      }
      Thread.sleep(1000);
    }
    Assert.fail();

  }

}
