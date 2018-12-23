package org.spf4j.zhelloworld;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.concurrent.DefaultScheduler;
import org.spf4j.jaxrs.client.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.Level;
import org.spf4j.test.log.Attachments;
import org.spf4j.test.log.LogCollection;
import org.spf4j.test.log.TestLogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.annotations.ExpectLog;

public class MyResourceTest {

  private static final Logger LOG = LoggerFactory.getLogger(MyResourceTest.class);

  private static HttpServer server;
  private static Spf4jWebTarget target;

  @BeforeClass
  public static void setUp() throws Exception {
    // start the server
    server = Main.startServer();
    // create the client
    Spf4JClient c = new Spf4JClient(ClientBuilder
            .newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .executorService(DefaultExecutor.instance())
            .scheduledExecutorService(DefaultScheduler.instance())
            .readTimeout(30, TimeUnit.SECONDS)
            .register(ExecutionContextClientFilter.class)
            .property(ClientProperties.USE_ENCODING, "gzip")
            .build());

    // uncomment the following line if you want to enable
    // support for JSON in the client (you also have to uncomment
    // dependency on jersey-media-json module in pom.xml and Main.startServer())
    // --
    // c.configuration().enable(new org.glassfish.jersey.media.json.JsonJaxbFeature());
    target = c.target(Main.BASE_URI);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.shutdownNow();
  }


  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  public void testHello() {
    for (int i = 0; i < 100; i++) {
      Invocation.Builder request = target.path("demo/myresource/hello").request();
      String responseMsg = request.get(String.class);
      Assert.assertThat(responseMsg, Matchers.startsWith("Hello world"));
    }
  }

  @Test
  public void testAHello() {
    Invocation.Builder request = target.path("demo/myresource/ahello").request();
    String responseMsg = request.get(String.class);
    Assert.assertThat(responseMsg, Matchers.startsWith("A Delayed hello"));
  }

  @Test(expected = TimeoutException.class, timeout = 1000)
  @ExpectLog(level = Level.ERROR, messageRegexp = "Done GET /myresource/aTimeout")
  public void testATimeoout() {
    try {
      String responseMsg = target.path("demo/myresource/aTimeout")
             .request().get(String.class);
      Assert.assertThat(responseMsg, Matchers.startsWith("A Delayed hello"));
    } catch (InternalServerErrorException ex) {
      LOG.debug("Expected Error Response", ex.getResponse().readEntity(String.class), ex);
    }
  }



  @Test
  public void testGetExecCtx() {
    Invocation.Builder request = target.path("demo/myresource/execContext")
            .request();
    String responseMsg = request.get(String.class);
    LOG.debug("Response", responseMsg);
  }

  @Test
  public void testGetError() {
    LogCollection<List<TestLogRecord>> col = TestLoggers.sys().collect("org.spf4j.servlet", Level.ERROR, true,
            Collectors.mapping((log) -> {
              log.attach(Attachments.ASSERTED);
              return log;
            }, Collectors.toList()));
    Invocation.Builder request = target.path("demo/myresource/error").request();
    try {
      request.get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException ex) {
      LOG.debug("Expected Error Response", ex.getResponse().readEntity(String.class), ex);
    }
    List<TestLogRecord> errors = col.get();
    Assert.assertThat(errors, Matchers.not(Matchers.emptyIterable()));
  }

  @Test
  public void testSplit() {
    LOG.debug("split ={}", "a   b c ".split("[ ]+"));
  }

  public ByteBuffer injectBinary(Tracer tr, final Span span) {
    ByteBuffer bb = ByteBuffer.allocate(1024);
    tr.inject(span.context(), Format.Builtin.BINARY, bb);
    bb.flip();
    return bb;
  }
}
