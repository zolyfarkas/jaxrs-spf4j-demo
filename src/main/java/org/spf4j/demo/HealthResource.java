
package org.spf4j.demo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author Zoltan Farkas
 */
@Path("health")
public class HealthResource {

  @GET
  @Path("ping")
  public void ping() {
  }

}
