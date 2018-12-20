package org.spf4j.zhelloworld;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.concurrent.DefaultContextAwareExecutor;
import org.spf4j.log.ExecContextLogger;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {


  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(MyResource.class));

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
          try {
              //Simulating a long running process
              Thread.sleep(2000);
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
          ar.resume("A Delayed hello");
      });
  }

  @GET
  @Path("aTimeout")
  @Produces(MediaType.TEXT_PLAIN)
  public void asyncTimeout(@Suspended final AsyncResponse ar) {
    ar.setTimeout(1, TimeUnit.SECONDS);
    DefaultContextAwareExecutor.instance().submit(() -> {
          try {
              //Simulating a long running process
              Thread.sleep(3000);
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
          ar.resume("A Delayed hello");
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
  public String getIt(@Context HttpHeaders headers) throws InterruptedException, TimeoutException {
      Thread.sleep(ThreadLocalRandom.current().nextInt(10));
      ExecutionContext ec = ExecutionContexts.current();
      return "Hello world " + ec.getName() + ", timeleft" + ec.getMillisToDeadline();
  }
}
