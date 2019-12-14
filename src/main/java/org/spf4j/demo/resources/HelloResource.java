package org.spf4j.demo.resources;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Timing;
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.jaxrs.CsvParam;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.log.ExecContextLogger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("helloResource")
public class HelloResource {

  private static final ExecContextLogger LOG = new ExecContextLogger(LoggerFactory.getLogger(HelloResource.class));

  private final Spf4JClient cl;

  private final int port;

  @Inject
  public HelloResource(final Spf4JClient cl, @ConfigProperty(name = "servlet.port") final int port) {
    this.cl = cl;
    this.port = port;
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("deadline")
  public long getDeadline() throws InterruptedException, TimeoutException {
    ExecutionContext ec = ExecutionContexts.current();
    return Timing.getCurrentTiming().fromNanoTimeToInstant(ec.getDeadlineNanos()).toEpochMilli();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("error")
  public String error() {
    throw new RuntimeException("some exception in " + this);
  }

  @GET
  @Path("ahello")
  @Produces(MediaType.TEXT_PLAIN)
  @Deprecated
  public void asyncHello(@Suspended final AsyncResponse ar) {
    DefaultContextAwareExecutor.instance().submit(() -> {
      ar.resume("A Delayed hello");
    });
  }

  @GET
  @Path("aTimeout")
  @Produces(MediaType.TEXT_PLAIN)
  public void asyncTimeout(@Suspended final AsyncResponse ar) throws TimeoutException {
    DefaultContextAwareExecutor.instance().submit(() -> {
      try {
        //Simulating a long running process
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      LOG.debug("Finished the async task");
      ar.resume("A Delayed hello");
    });
  }

  @GET
  @Path("aError")
  @Produces(MediaType.TEXT_PLAIN)
  public void asyncError(@Suspended final AsyncResponse ar) {
    DefaultContextAwareExecutor.instance().submit(() -> {
      ar.resume(new RuntimeException("A test error !"));
    });
  }


  @GET
  @Path("errorCustom")
  @Produces(MediaType.TEXT_PLAIN)
  public String customError() {
     throw new
        RuntimeException(new ServerErrorException(Response.status(500).entity(Arrays.asList("A","B")).build()));
  }

  /**
   * Method handling HTTP GET requests. The returned object will be sent to the client as "text/plain" media type.
   *
   * @return String that will be returned as a text/plain response.
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("hello")
  public String hello() throws InterruptedException, TimeoutException {
    Thread.sleep(ThreadLocalRandom.current().nextInt(10));
    ExecutionContext ec = ExecutionContexts.current();
    return "Hello world " + ec.getName() + ", timeleft " + ec.getMillisToDeadline() + " ms";
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("slowHello")
  public String slowHello(@QueryParam("time") @DefaultValue("2") final int timeSeconds)
          throws InterruptedException, TimeoutException {
    long millisToSleep = TimeUnit.SECONDS.toMillis(timeSeconds);
    someMethodA(millisToSleep / 2);
    someMethodB(millisToSleep / 2);
    ExecutionContext ec = ExecutionContexts.current();
    return "Slow Hello world " + ec.getName() + ", timeleft " + ec.getMillisToDeadline() + " ms";
  }

  private void someMethodA(final long sleepMillis) throws InterruptedException {
    Thread.sleep(sleepMillis / 2);
    someMethodC(sleepMillis / 2);
  }

  private void someMethodB(final long sleepMillis) throws InterruptedException {
     Thread.sleep(sleepMillis);
  }

  private void someMethodC(final long sleepMillis) throws InterruptedException {
     Thread.sleep(sleepMillis);
  }


  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("slowBrokenHello")
  public String slowBrokenHello(@QueryParam("time") @DefaultValue("2") final int timeSeconds)
          throws InterruptedException, TimeoutException {
    Thread.sleep(TimeUnit.SECONDS.toMillis(timeSeconds));
    throw new RuntimeException();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyHelloWorld")
  public void flakyHelloWorld(@Suspended final AsyncResponse ar) throws InterruptedException, TimeoutException {
    beFlaky();
    Spf4jWebTarget base = cl.target("http://localhost:" + port).path("helloResource");
    base.path("flakyHello").request(MediaType.TEXT_PLAIN).rx().get(String.class)
            .thenCombine(base.path("flakyWorld").request(MediaType.TEXT_PLAIN).rx().get(String.class),
                    (h, w) -> h + ' ' + w
            ).whenComplete((r, t) -> {
              if (t != null) {
                ar.resume(t);
              } else {
                ar.resume(r);
              }
            });
  }


  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("buggyHelloWorld")
  public void buggyHelloWorld(@Suspended final AsyncResponse ar) throws InterruptedException, TimeoutException {
    Spf4jWebTarget base = cl.target("http://localhost:" + port).path("404");
    CompletionStage<String> cf1 = base.path("flakyHello").request(MediaType.TEXT_PLAIN).rx().get(String.class);
    CompletionStage<String> cf2 = base.path("flakyWorld").request(MediaType.TEXT_PLAIN).rx().get(String.class);
    cf1.thenCombine(cf2, (h, w) -> h + ' ' + w
    ).whenComplete((r, t) -> {
      LOG.debug("Result received {}", r, t);
      if (t != null) {
        ar.resume(t);
      } else {
        ar.resume(r);
      }
    });
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyHelloWorldSync")
  public String flakyHelloWorldSync() throws InterruptedException, TimeoutException {
    beFlaky();
    Spf4jWebTarget base = cl.target("http://localhost:" + port).path("helloResource");
    return base.path("flakyHello").request(MediaType.TEXT_PLAIN).get(String.class)
            + ' ' + base.path("flakyWorld").request(MediaType.TEXT_PLAIN).get(String.class);
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyHello")
  public String flakyHello() throws InterruptedException, TimeoutException {
    beFlaky();
    return "Hello";
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyWorld")
  public String flakyWorld() throws InterruptedException, TimeoutException {
    beFlaky();
    return "World";
  }

  private static void beFlaky() throws ServiceUnavailableException, InterruptedException {
    int randomNr = ThreadLocalRandom.current().nextInt(10);
    if (randomNr < 3) {
      throw new ServiceUnavailableException(0L);
    } else if (randomNr < 6) {
      Thread.sleep(1000);
    }
  }

  @GET
  @Produces("application/json")
  @Path("csvListParamEcho")
  public List<Integer> echoList(@CsvParam @QueryParam("lp") List<Integer> param)
          throws InterruptedException, TimeoutException {
    return param;
  }

}
