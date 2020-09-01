package org.spf4j.demo.resources;


import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.demo.config.DemoConfig;


/**
 * a live configurable demo resource.
 */
@Path("hello")
@PermitAll
public class HelloConfigResource {


  private final Supplier<Boolean> helloFlag;

  private final Supplier<DemoConfig> demoConfig;

  @Inject
  public HelloConfigResource(@ConfigProperty(name = "hello.feature") final Supplier<Boolean> helloFlag,
          @ConfigProperty(name = "demo.config") final Supplier<DemoConfig> demoConfig) {
    this.helloFlag = helloFlag;
    this.demoConfig = demoConfig;
  }


  /**
   * Method handling HTTP GET requests. The returned object will be sent to the client as "text/plain" media type.
   *
   * @return String that will be returned as a text/plain response.
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() throws InterruptedException, TimeoutException {
    if (helloFlag.get()) {
      return "Hi how are you?";
    } else {
      return demoConfig.get().getStrVal();
    }
  }



}
