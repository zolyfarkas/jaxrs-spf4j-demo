package org.spf4j.zhelloworld;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.concurrent.DefaultContextAwareScheduledExecutor;
import org.spf4j.jaxrs.client.ExecutionContextClientFilter;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.jaxrs.common.CustomExecutorServiceProvider;
import org.spf4j.jaxrs.common.CustomScheduledExecutionServiceProvider;
import org.spf4j.log.ExecContextLogger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {

  private static final ExecContextLogger LOG = new ExecContextLogger(LoggerFactory.getLogger(MyResource.class));

  private final Spf4JClient cl = new Spf4JClient(ClientBuilder
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


  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("execContext")
  public String getIt() throws InterruptedException, TimeoutException {
      ExecutionContext ec = ExecutionContexts.current();
      StringBuilder sb = new StringBuilder();
      ec.writeTo(sb);
      return sb.toString();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("error")
  public String error()  {
    LOG.debug("entered error method");
    throw new RuntimeException("some exception in " + this);
  }


  @GET
  @Path("ahello")
  @Produces(MediaType.TEXT_PLAIN)
  public void asyncHello(@Suspended final AsyncResponse ar) {
    DefaultContextAwareExecutor.instance().submit(() -> {
          ar.resume("A Delayed hello");
      });
  }

  @GET
  @Path("aTimeout")
  @Produces(MediaType.TEXT_PLAIN)
  public void asyncTimeout(@Suspended final AsyncResponse ar) {
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
      return "Hello world " + ec.getName() + ", timeleft" + ec.getMillisToDeadline();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyHelloWorld")
  public void flakyHelloWorld(@Suspended final AsyncResponse ar) throws InterruptedException, TimeoutException {
      if (ThreadLocalRandom.current().nextInt(10) > 3) {
        throw new ServiceUnavailableException(0L);
      }
      Spf4jWebTarget base = cl.target(Main.BASE_URI).path("demo/myresource");
      base.path("flakyHello").request(MediaType.TEXT_PLAIN).rx().get(String.class)
              .thenCombine(base.path("flakyWorld").request(MediaType.TEXT_PLAIN).rx().get(String.class),
                      (h, w) -> h + ' ' + w
              ).whenComplete((r,  t) -> {
                        if (t != null) {
                          ar.resume(t);
                        } else {
                          ar.resume(r);
                        }
                      });
  }


  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyHello")
  public String flakyHello() throws InterruptedException, TimeoutException {
      if (ThreadLocalRandom.current().nextInt(10) > 3) {
        throw new ServiceUnavailableException(0L);
      }
      return "Hello";
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("flakyWorld")
  public String flakyWorld() throws InterruptedException, TimeoutException {
      if (ThreadLocalRandom.current().nextInt(10) > 3) {
        throw new ServiceUnavailableException(0L);
      }
      return "World";
  }



}
