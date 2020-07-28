
package org.spf4j.demo.resources;

import java.util.Collections;
import javax.annotation.security.PermitAll;
import javax.ws.rs.Path;
import org.spf4j.jaxrs.server.resources.ClassPathResource;

/**
 * @author Zoltan Farkas
 */
@Path("/")
public class StaticResources {

  private final ClassPathResource resource;

  public StaticResources() {
    resource = new ClassPathResource("static", Collections.singletonList("index.html"));
  }

  @Path("")
  @PermitAll
  public ClassPathResource getRoot() {
    return resource;
  }

}
