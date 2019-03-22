package org.spf4j.actuator.health;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.actuator.health.HealthCheck.Type;
import org.spf4j.base.avro.HealthCheckInfo;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;
import org.spf4j.jaxrs.server.DebugDetailEntitlement;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.jaxrs.ConfigProperty;

/**
 * @author Zoltan Farkas
 */
@Path("health")
@Produces(value = {"application/avro-x+json", "application/json",
  "application/avro+json", "application/avro", "application/octet-stream"})
public final class HealthResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(HealthResource.class));

  private final HealthOrgNode checks;

  private final String host;

  private final DebugDetailEntitlement ddEnt;

  @Inject
  public HealthResource(final Iterable<HealthCheck.Registration> healthChecks,
          final DebugDetailEntitlement ddEnt,
          @ConfigProperty("hostName") @DefaultValue("hostName") final String host) {
    this.ddEnt = ddEnt;
    this.host = host;
    checks =  HealthOrgNode.newHealthChecks();
    for (HealthCheck.Registration registration : healthChecks) {
      String[] path = registration.getPath();
      HealthCheck check = registration.getCheck();
      switch (check.getType()) {
        case local:
          path = org.spf4j.base.Arrays.preppend(path, Type.local.toString());
          break;
        case cluster:
          path = org.spf4j.base.Arrays.preppend(path, Type.cluster.toString());
          break;
        case custom:
          break;
        default:
          throw new UnsupportedOperationException("Unsupported health check type " + check.getType());
      }
      checks.addHealthCheck(check, path);
    }
  }

  @GET
  @Path("ping")
  public void ping() {
    // DO nothing, this is a ping endpoint.
  }

  @GET
  @Path("info")
  public HealthCheckInfo list(@QueryParam("maxDepth") @DefaultValue("10") final int maxDepth) {
    return list(Collections.EMPTY_LIST, maxDepth);
  }


  @GET
  @Path("info/{path:.*}")
  public HealthCheckInfo list(@PathParam("path") final List<String> path,
          @QueryParam("maxDepth") @DefaultValue("10") final int maxDepth) {
    String[] pathArr = path.toArray(new String[path.size()]);
    HealthOrgNode hNode = checks.getHealthNode(pathArr);
    if (hNode == null) {
        throw new NotFoundException("No health checks at " + path);
    }
    return hNode.getHealthCheckInfo("", maxDepth);
  }

  @GET
  @Path("check")
  public Response run(
          @QueryParam("debug") @DefaultValue("false") final boolean pisDebug,
          @QueryParam("debugOnError") @DefaultValue("true") final boolean pisDebugOnError,
          @Context SecurityContext secCtx) {
    return run(Collections.EMPTY_LIST, pisDebug, pisDebugOnError, secCtx);
  }

  @GET
  @Path("check/{path:.*}")
  public Response run(@PathParam("path") final List<String> path,
          @QueryParam("debug") @DefaultValue("false") final boolean pisDebug,
          @QueryParam("debugOnError") @DefaultValue("true") final boolean pisDebugOnError,
          @Context SecurityContext secCtx) {
    boolean isDebug = pisDebug;
    boolean isDebugOnError =  pisDebugOnError;
    if (!ddEnt.test(secCtx)) {
      isDebug = false;
      isDebugOnError = false;
    }
    String[] pathArr = path.toArray(new String[path.size()]);
    HealthOrgNode hNode = checks.getHealthNode(pathArr);
    HealthRecord healthRecord = hNode.getHealthRecord("", host, LOG, isDebug, isDebugOnError);
    if (healthRecord.getStatus() == HealthStatus.HEALTHY) {
      return Response.ok(healthRecord).build();
    } else {
      return Response.status(500).entity(healthRecord).build();
    }
  }

}
