
package org.spf4j.demo.resources;

import java.util.Collections;
import javax.ws.rs.Path;
import org.spf4j.jaxrs.server.resources.ClassPathResource;

/**
 *
 * @author Zoltan Farkas
 */
@Path("/")
public class StaticResources {

  private final ClassPathResource resource;

  public StaticResources() {
    resource = new ClassPathResource("static", Collections.singletonList("index.html"));
  }

  @Path("")
  public ClassPathResource getRoot() {
    return resource;
  }

}
