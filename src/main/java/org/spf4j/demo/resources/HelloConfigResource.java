package org.spf4j.demo.resources;


import java.io.Closeable;
import java.util.concurrent.TimeoutException;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.spf4j.demo.config.DemoConfig;
import org.spf4j.jaxrs.config.ObservableSupplier;


/**
 * a live configurable demo resource.
 */
@Path("hello")
@PermitAll
@Singleton
public class HelloConfigResource implements Closeable {


  private final ObservableSupplier<Boolean> helloFlag;

  private final ObservableSupplier<DemoConfig> demoConfig;

  @Inject
  public HelloConfigResource(@ConfigProperty(name = "hello.feature") final ObservableSupplier<Boolean> helloFlag,
          @ConfigProperty(name = "demo.config") final ObservableSupplier<DemoConfig> demoConfig) {
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

  @Override
  @PreDestroy
  public void close() {
    this.demoConfig.close();
    this.helloFlag.close();
  }


}
