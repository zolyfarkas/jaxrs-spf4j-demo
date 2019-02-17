
package org.spf4j.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.PathSegment;
import org.spf4j.base.Either;
import org.spf4j.base.avro.HealthRecord;

/**
 * @author Zoltan Farkas
 */
@Path("health")
public final class HealthResource {


  private final Map<String, Object> checks;

  @Inject
  public HealthResource(final Iterable<HealthCheck.Registration> healthChecks) {
    checks = new HashMap<>();
    for (HealthCheck.Registration registration : healthChecks) {
      String[] path = registration.getPath();
      Map<String, Object> toPut = checks;
      for (int i = 0; i < path.length - 1; i++) {
        String seg = path[i];
        Map<String, Object> otp = toPut;
        toPut = (Map<String, Object>) toPut.get(seg);
        if (toPut == null) {
          toPut = new HashMap<>();
          otp.put(seg, toPut);
        }
      }
      toPut.put(path[path.length - 1], registration.getCheck());
    }
  }


  @GET
  @Path("ping")
  public void ping() {
     // DO nothing, this is a ping endpoint.
  }

  @GET
  @Path("record/{healthPath:.*}")
  public HealthRecord record(@PathParam("healthPath") final List<PathSegment> path) {
    return HealthRecord.newBuilder().build();
  }

}
