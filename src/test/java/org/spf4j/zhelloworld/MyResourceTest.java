package org.spf4j.zhelloworld;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.concurrent.DefaultContextAwareScheduledExecutor;
import org.spf4j.jaxrs.client.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.jaxrs.common.CustomExecutorServiceProvider;
import org.spf4j.jaxrs.common.CustomScheduledExecutionServiceProvider;
import org.spf4j.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.TestLogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.matchers.LogMatchers;

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
            .executorService(DefaultContextAwareExecutor.instance())
            .scheduledExecutorService(DefaultContextAwareScheduledExecutor.instance())
            .readTimeout(30, TimeUnit.SECONDS)
            .register(ExecutionContextClientFilter.class)
            .register(CustomExecutorServiceProvider.class)
            .register(CustomScheduledExecutionServiceProvider.class)
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

  @Test
  public void testFlakyHello() throws InterruptedException, ExecutionException, TimeoutException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern("Done GET /myresource/flakyHello"),
            Matchers.any((Class) Iterable.class));
    Invocation.Builder request = target.path("demo/myresource/flakyHello").request();
    Future<String> responseMsg = request.buildGet().submit(String.class);
    Assert.assertThat(responseMsg.get(2, TimeUnit.SECONDS), Matchers.startsWith("Hello world"));
    expect.assertObservation();
  }


  @Test(timeout = 10000)
  public void testATimeoout() {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.WARN,
            true, LogMatchers.hasMessageWithPattern("Done GET /myresource/aTimeout"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      target.path("demo/myresource/aTimeout")
             .request()
              .withTimeout(500, TimeUnit.MILLISECONDS)
              .get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException | UncheckedTimeoutException ex) {
      LOG.debug("Expected Error Response", ex);
    } finally {
      expect.assertObservation(10, TimeUnit.SECONDS);
    }
  }


  @Test(timeout = 10000)
  public void testAError() {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern("Done GET /myresource/aError"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      target.path("demo/myresource/aError")
             .request()
              .withTimeout(500, TimeUnit.MILLISECONDS)
              .get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException | UncheckedTimeoutException ex) {
      LOG.debug("Expected Error Response", ex);
    } finally {
      expect.assertObservation(10, TimeUnit.SECONDS);
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
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, Matchers.any(TestLogRecord.class),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    Invocation.Builder request = target.path("demo/myresource/error").request();
    try {
      request.get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException ex) {
      LOG.debug("Expected Error Response", ex.getResponse().readEntity(String.class), ex);
    }
    expect.assertObservation();
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
