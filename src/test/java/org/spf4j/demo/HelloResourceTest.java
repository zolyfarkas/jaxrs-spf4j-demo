package org.spf4j.demo;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.GenericType;

import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.UncheckedTimeoutException;
import org.spf4j.base.avro.RemoteException;
import org.spf4j.service.avro.ServiceError;
import org.spf4j.concurrent.DefaultExecutor;
import org.spf4j.failsafe.avro.TimeoutRelativeHedgePolicy;
import org.spf4j.jaxrs.client.Spf4jInvocationBuilder;
import org.spf4j.log.Level;
import org.spf4j.test.log.LogAssert;
import org.spf4j.test.log.TestLogRecord;
import org.spf4j.test.log.TestLoggers;
import org.spf4j.test.log.annotations.ExpectLog;
import org.spf4j.test.matchers.LogMatchers;

public class HelloResourceTest extends ServiceIntegrationBase {

  private static final Logger LOG = LoggerFactory.getLogger(HelloResourceTest.class);

  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  public void testHello() {
    for (int i = 0; i < 100; i++) {
      Invocation.Builder request = getTarget().path("helloResource/hello").request();
      String responseMsg = request.get(String.class);
      Assert.assertThat(responseMsg, Matchers.startsWith("Hello world"));
    }
  }

  @Test
  public void testEchoCsvList() {
      List<Integer> expected = Arrays.asList(1,2,3,4);
      Invocation.Builder request = getTarget().path("helloResource/csvListParamEcho")
              .queryParam("lp", expected.stream().map((x) -> x.toString()).collect(Collectors.joining(",")))
              .request().accept("application/json");
      List<Integer> result = request.get(new GenericType<List<Integer>>() {});
      Assert.assertEquals(expected, result);
  }


  @Test
  public void test404() {
      Invocation.Builder request = getTarget().path("helloResource/helloLalaland")
              .request()
              .accept("text/html", "application/xhtml+xml", "application/xml;q=0.9", "*/*;q=0.8");
      try {
        request.get(new GenericType<String>() {});
        Assert.fail();
      } catch (WebApplicationException ex) {
        LOG.debug("expected exception encoded as {}", ex.getResponse().getMediaType(), ex);
        Assert.assertThat(ex.getResponse().getMediaType().toString(), Matchers.containsString("json"));
      }
  }

  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  @ExpectLog(level = Level.WARN, messageRegexp = "Done GET.*")
  public void testSlowHello() {
    Spf4jInvocationBuilder request = getTarget().path("helloResource/slowHello")
            .request().withTimeout(3000, TimeUnit.MILLISECONDS);
    String responseMsg = request.get(String.class);
    Assert.assertThat(responseMsg, Matchers.startsWith("Slow Hello world"));
  }


  /**
   * Test to see that the message "Got it!" is sent in the response.
   */
  @Test
  @ExpectLog(level = Level.ERROR, messageRegexp = "Done GET.*")
  public void testSlowBrokenHello() {
    Spf4jInvocationBuilder request = getTarget().path("helloResource/slowBrokenHello")
            .request().withTimeout(3, TimeUnit.SECONDS);
    try {
      request.get(String.class);
      Assert.fail();
    } catch (RuntimeException ex) {
      LOG.debug("Expected", ex);
    }
  }


  @Test
  public void testAHello() {
    Invocation.Builder request = getTarget().path("helloResource/ahello").request();
    String responseMsg = request.get(String.class);
    Assert.assertThat(responseMsg, Matchers.startsWith("A Delayed hello"));
  }

  @Test
  public void testCustomError() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern(".*"),
            Matchers.any((Class) Iterable.class));
    Invocation.Builder request = getTarget().path("helloResource/errorCustom").request();
    try {
      request.get(String.class);
      Assert.fail();
    } catch (WebApplicationException ex) {
      ServiceError sErr = (ServiceError) ex.getResponse().getEntity();
      LOG.debug("Expected error ", sErr);
    }
     expect.assertObservation(3, TimeUnit.SECONDS);
  }


  @Test
  public void testFlakyHelloWorld() throws InterruptedException, ExecutionException, TimeoutException {
    LogAssert expect = TestLoggers.sys().expect("", Level.ERROR,
            false, LogMatchers.hasMessageWithPattern(".*"),
            Matchers.any((Class) Iterable.class));
    Spf4jInvocationBuilder request = getClient()
            .target(getLocalService()).path("helloResource/flakyHelloWorld").request();
    request.withHedgePolicy(new TimeoutRelativeHedgePolicy(Duration.ofNanos(100), Duration.ofNanos(1000), 0.6, 2));
    Future<String> responseMsg = request.withTimeout(3000, TimeUnit.SECONDS)
            .buildGet().submit(String.class);
    Assert.assertThat(responseMsg.get(3000, TimeUnit.SECONDS), Matchers.startsWith("Hello World"));
    LOG.info("Finished Flaky test");
    expect.assertObservation(3, TimeUnit.SECONDS);
  }


  @Test
  public void testBuggyHelloWorld() throws InterruptedException, ExecutionException, TimeoutException {
    // this test may or may not log an error.
    LogAssert expect = TestLoggers.sys().expect("", Level.INFO,
            false, LogMatchers.hasMessageWithPattern(".*"),
            Matchers.any((Class) Iterable.class));
    Spf4jInvocationBuilder request = getClient()
        .target(getLocalService()).path("helloResource/buggyHelloWorld").request()
            .noDefaultRetryPolicy();
    try {
        request
            .withTimeout(30000, TimeUnit.SECONDS)
            .buildGet().invoke(String.class);
      Assert.fail();
    } catch (RuntimeException ex) {
      //expected
      LOG.debug("Expected exception", ex);
    }
    LOG.info("Finished buggy test");
    expect.assertObservation(10, TimeUnit.SECONDS);
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
    Spf4jInvocationBuilder request = getClient()
        .target(getLocalService()).path("helloResource/flakyHelloWorldSync").request()
            .withTimeout(5, TimeUnit.SECONDS)
            .withHedgePolicy(new TimeoutRelativeHedgePolicy(Duration.ofNanos(100), Duration.ofNanos(1000), 0.6, 2));
    String responseMsg = request.get(String.class);
    Assert.assertThat(responseMsg, Matchers.startsWith("Hello World"));
    LOG.info("Finished Flaky test");
    expect.assertObservation();
  }


  @Test(timeout = 10000000)
  public void testATimeoout() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.WARN,
            true, LogMatchers.hasMessageWithPattern("Done GET.*"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      getTarget().path("helloResource/aTimeout")
             .request()
              .withTimeout(500, TimeUnit.MILLISECONDS)
              .get(String.class);
      Assert.fail();
    } catch (InternalServerErrorException | UncheckedTimeoutException | ProcessingException ex) {
      LOG.debug("Expected Error Response", ex);
    } finally {
      Thread.sleep(2000);
      expect.assertObservation(10, TimeUnit.SECONDS);
    }
  }


  @Test(timeout = 25000)
  public void testAError() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern("Done GET.*aError"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      getTarget().path("helloResource/aError")
             .request()
              .withTimeout(10000, TimeUnit.MILLISECONDS)
              .get(String.class);
      Assert.fail();
    } catch (WebApplicationException | UncheckedTimeoutException | ResponseProcessingException  ex) {
      LOG.debug("Expected Error Response", ex);
    } finally {
      expect.assertObservation(10, TimeUnit.SECONDS);
    }
  }

  @Test(timeout = 10000)
  public void testAError2() throws InterruptedException {
    LogAssert expect = TestLoggers.sys().expect("org.spf4j.servlet", Level.ERROR,
            true, LogMatchers.hasMessageWithPattern("Done GET.*/helloResource/aError"),
            Matchers.not(Matchers.emptyIterableOf(TestLogRecord.class)));
    try  {
      getTarget().path("helloResource/aError")
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
    Spf4jInvocationBuilder request = getTarget().path("helloResource/deadline")
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
    Invocation.Builder request = getTarget().path("helloResource/error").request();
    try {
      request.get(String.class);
      Assert.fail();
    } catch (WebApplicationException ex) {
      LOG.debug("Expected Error Response", ex.getResponse().getEntity(), ex);
    }
    expect.assertObservation();
  }


}
