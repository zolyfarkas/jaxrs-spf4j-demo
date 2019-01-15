package org.spf4j.demo;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.client.Invocation;

import org.glassfish.grizzly.http.server.HttpServer;
import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.base.avro.RemoteException;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.failsafe.HedgePolicy;
import org.spf4j.failsafe.TimeoutRelativeHedge;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jInvocationBuilder;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.Level;
import org.spf4j.stackmonitor.Sampler;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.TestLogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.matchers.LogMatchers;

public class HelloResourceTest {

  private static final Logger LOG = LoggerFactory.getLogger(HelloResourceTest.class);

  private static HttpServer server;
  private static Spf4jWebTarget target;
  private static Spf4JClient client;
  private static Sampler profiler;

  @BeforeClass
  public static void setUp() throws Exception {
    // start the server
    profiler = Main.startProfiler();
    server = Main.startHttpServer();
    client = DemoApplication.getInstance().getRestClient();
    target = client.target(Main.BASE_URI);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.shutdownNow();
    profiler.dispose();
  }


  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  public void testHello() {
    for (int i = 0; i < 100; i++) {
      Invocation.Builder request = target.path("demo/helloResource/hello").request();
      String responseMsg = request.get(String.class);
      Assert.assertThat(responseMsg, Matchers.startsWith("Hello world"));
    }
  }

  @Test
  public void testAHello() {
    Invocation.Builder request = target.path("demo/helloResource/ahello").request();
    String responseMsg = request.get(String.class);
    Assert.assertThat(responseMsg, Matchers.startsWith("A Delayed hello"));
  }

  @Test
  public void testCustomError() {
    LogAssert expect = TestLoggers.sys().expect("", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern(".*"),
            Matchers.any((Class) Iterable.class));
    Invocation.Builder request = target.path("demo/helloResource/errorCustom").request();
    try {
      request.get(String.class);
      Assert.fail();
    } catch (ServerErrorException ex) {
      ServiceError sErr = ex.getResponse().readEntity(ServiceError.class);
      LOG.debug("Expected error ", sErr);
    }
     expect.assertObservation(3, TimeUnit.SECONDS);
  }


  @Test
  public void testFlakyHelloWorld() throws InterruptedException, ExecutionException, TimeoutException {
    LogAssert expect = TestLoggers.sys().expect("", Level.ERROR,
            false, LogMatchers.hasMessageWithPattern(".*"),
            Matchers.any((Class) Iterable.class));
    Spf4jInvocationBuilder request = client.withHedgePolicy(new TimeoutRelativeHedge(6, TimeUnit.MILLISECONDS.toNanos(100),
            TimeUnit.MILLISECONDS.toNanos(200), 2))
            .target(Main.BASE_URI).path("demo/helloResource/flakyHelloWorld").request();
    Future<String> responseMsg = request.withTimeout(3000, TimeUnit.SECONDS)
            .buildGet().submit(String.class);
    Assert.assertThat(responseMsg.get(3000, TimeUnit.SECONDS), Matchers.startsWith("Hello World"));
    LOG.info("Finished Flaky test");
    expect.assertObservation(3, TimeUnit.SECONDS);
  }


  @Test
  public void testBuggyHelloWorld() throws InterruptedException, ExecutionException, TimeoutException {
    Spf4jInvocationBuilder request = client.withHedgePolicy(HedgePolicy.NONE)
        .target(Main.BASE_URI).path("demo/helloResource/buggyHelloWorld").request();
    try {
        request
            .withTimeout(3000, TimeUnit.SECONDS)
            .buildGet().invoke(String.class);
      Assert.fail();
    } catch (RuntimeException ex) {
      //expected
      LOG.debug("Excepted exception", ex);
    }
    LOG.info("Finished buggy test");
  }

  @Test
  public void completableFuturestest() throws InterruptedException, ExecutionException {
    CompletableFuture<String> handle = CompletableFuture.supplyAsync(() -> {
      throw new RuntimeException("ex 1");
    }).thenCombine(CompletableFuture.supplyAsync(() -> {
      throw new RuntimeException("ex 2");
    }), (x, y) -> "=" + x + y)
            .handle((r, t) ->  "yupee" + r + t);
    LOG.debug(handle.get());
  }

  @Test
  public void completableFuturestest2() throws InterruptedException, ExecutionException {
    CompletableFuture<String> f1 = new CompletableFuture<>();
    CompletableFuture<String> f2 = new CompletableFuture<>();
    CompletableFuture<String> handle = f1.handle((r,  t) -> {
      throw new RuntimeException(t);
    }).thenCombine(f2.handle((r,  t) -> {
      throw new RuntimeException(t);
    }), (x, y) -> "=" + x + y)
            .handle((r, t) ->  "yupee" + r + t);
    DefaultExecutor.INSTANCE.submit(() -> f1.completeExceptionally(new RuntimeException("ex 1")));
    DefaultExecutor.INSTANCE.submit(() -> f2.completeExceptionally(new RuntimeException("ex 2")));
    LOG.debug(handle.get());
  }

  @Test
  public void completableFuturestest3() throws InterruptedException, ExecutionException {
    CompletableFuture f1 = new CompletableFuture();
    CompletableFuture f2 = new CompletableFuture();
    DefaultExecutor.INSTANCE.submit(() -> f1.completeExceptionally(new RuntimeException("ex 1")));
    DefaultExecutor.INSTANCE.submit(() -> f2.completeExceptionally(new RuntimeException("ex 2")));
    CompletableFuture<String> handle = f1.thenCombine(f2, (x, y) -> "=" + x + y)
            .handle((r, t) ->  "yupee" + r + t);
    LOG.debug(handle.get());
  }


  @Test
  public void testFlakyHelloWorldSync() throws InterruptedException, ExecutionException, TimeoutException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            false, LogMatchers.hasMessageWithPattern("Done GET/helloResource.*"),
            Matchers.any((Class) Iterable.class));
    Spf4jInvocationBuilder request = client.withHedgePolicy(new TimeoutRelativeHedge(6, TimeUnit.MILLISECONDS.toNanos(100),
        TimeUnit.MILLISECONDS.toNanos(200), 2))
        .target(Main.BASE_URI).path("demo/helloResource/flakyHelloWorldSync").request()
            .withTimeout(5, TimeUnit.SECONDS);
    String responseMsg = request.get(String.class);
    Assert.assertThat(responseMsg, Matchers.startsWith("Hello World"));
    LOG.info("Finished Flaky test");
    expect.assertObservation();
  }


  @Test(timeout = 10000)
  public void testATimeoout() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.WARN,
            true, LogMatchers.hasMessageWithPattern("Done GET.*"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      target.path("demo/helloResource/aTimeout")
             .request()
              .withTimeout(500, TimeUnit.MILLISECONDS)
              .get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException | UncheckedTimeoutException ex) {
      LOG.debug("Expected Error Response", ex);
    } finally {
      Thread.sleep(2000);
      expect.assertObservation(10, TimeUnit.SECONDS);
    }
  }


  @Test(timeout = 1000000)
  public void testAError() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern("Done GET/helloResource/aError"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      target.path("demo/helloResource/aError")
             .request()
              .withTimeout(500000, TimeUnit.MILLISECONDS)
              .get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException | UncheckedTimeoutException ex) {
      LOG.debug("Expected Error Response", ex);
      Assert.assertEquals(RemoteException.class, com.google.common.base.Throwables.getRootCause(ex).getClass());
    } finally {
      Thread.sleep(2000);
      expect.assertObservation(10, TimeUnit.SECONDS);
    }
  }

  @Test(timeout = 10000)
  public void testAError2() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern("Done GET/helloResource/aError"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      target.path("demo/helloResource/aError")
             .request()
              .withTimeout(500, TimeUnit.MILLISECONDS)
              .async().get(String.class).get();
      Assert.fail();
    } catch (ExecutionException ex) {
      LOG.debug("Expected Error Response", ex);
      Assert.assertEquals(RemoteException.class, com.google.common.base.Throwables.getRootCause(ex).getClass());
    } finally {
      expect.assertObservation(10, TimeUnit.SECONDS);
    }
  }


  @Test
  public void testGetDeadline() throws InterruptedException {
    Spf4jInvocationBuilder request = target.path("demo/helloResource/deadline")
            .request();
    long deadline = System.currentTimeMillis() + 2000;
    Long responseMsg = request
            .withTimeout(2, TimeUnit.SECONDS)
            .get(Long.class);
    deadline = System.currentTimeMillis() + 2000;
    responseMsg = request
            .withTimeout(2, TimeUnit.SECONDS)
            .get(Long.class);
    LOG.debug("Deadline is {}", responseMsg);
    Assert.assertTrue(Math.abs(deadline - responseMsg) < 2);
  }

  @Test
  public void testGetError() {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, Matchers.any(TestLogRecord.class),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    Invocation.Builder request = target.path("demo/helloResource/error").request();
    try {
      request.get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException ex) {
      LOG.debug("Expected Error Response", ex.getResponse().readEntity(ServiceError.class), ex);
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
