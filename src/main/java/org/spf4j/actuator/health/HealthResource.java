package org.spf4j.actuator.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;
import org.spf4j.base.avro.PathEntry;
import org.spf4j.jaxrs.server.DebugDetailEntitlement;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.jaxrs.ConfigProperty;

/**
 * @author Zoltan Farkas
 */
@Path("health")
public final class HealthResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(HealthResource.class));

  private final Map<String, Object> checks;

  private final String host;

  private final DebugDetailEntitlement ddEnt;

  @Inject
  public HealthResource(final Iterable<HealthCheck.Registration> healthChecks,
          final DebugDetailEntitlement ddEnt,
          @ConfigProperty("spf4j.jaxrs.serverHost") @DefaultValue("spf4j.jaxrs.serverHost") final String host) {
    this.ddEnt = ddEnt;
    this.host = host;
    checks = new HashMap<>();
    for (HealthCheck.Registration registration : healthChecks) {
      String[] path = registration.getPath();
      Map<String, Object> toPut = checks;
      int lIdx = path.length - 1;
      for (int i = 0; i < lIdx; i++) {
        String seg = path[i];
        Map<String, Object> otp = toPut;
        toPut = (Map<String, Object>) toPut.get(seg);
        if (toPut == null) {
          toPut = new HashMap<>();
          otp.put(seg, toPut);
        }
      }
      toPut.put(path[lIdx], registration.getCheck());
    }
  }

  @GET
  @Path("ping")
  public void ping() {
    // DO nothing, this is a ping endpoint.
  }


  @GET
  @Path("info/{path:.*}")
  public Response checks(@PathParam("path") final List<PathSegment> path) {
    Object aChecks = checks;
    for (PathSegment seg : path) {
      if (aChecks instanceof Map) {
        aChecks = ((Map<String, Object>) aChecks).get(seg.getPath());
      } else {
        throw new NotFoundException("No health checks at " + path);
      }
    }
    if (aChecks instanceof HealthCheck) {
      return Response.ok(((HealthCheck) aChecks).info()).build();
    } else if (aChecks instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) aChecks;
      List<PathEntry> entries = new ArrayList<>();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof HealthCheck) {
          entries.add(new PathEntry(entry.getKey(), false));
        } else {
          entries.add(new PathEntry(entry.getKey(), true));
        }
      }
      return Response.ok(new GenericEntity<List<PathEntry>>(entries){}).build();
    } else {
      throw new IllegalStateException("Illegal entry " + aChecks);
    }
  }


  @GET
  @Path("check/{path:.*}")
  public HealthRecord record(@PathParam("path") final List<PathSegment> path,
          @QueryParam("exclude") final List<String> excludeChecks,
          @QueryParam("debug") @DefaultValue("false") final boolean pisDebug,
          @QueryParam("debugOnError") @DefaultValue("true") final boolean pisDebugOnError,
          @Context SecurityContext secCtx) {
    boolean isDebug = pisDebug;
    boolean isDebugOnError =  pisDebugOnError;
    if (!ddEnt.test(secCtx)) {
      isDebug = false;
      isDebugOnError = false;
    }
    Object aChecks = checks;
    String name = "";
    for (PathSegment seg : path) {
      if (aChecks instanceof Map) {
        name = seg.getPath();
        aChecks = ((Map<String, Object>) aChecks).get(name);
      } else {
        throw new NotFoundException("No health checks at " + path);
      }
    }
    if (aChecks instanceof HealthCheck) {
      return check(name, (HealthCheck) aChecks, isDebug, isDebugOnError);
    } else if (aChecks instanceof Map) {
      return check(name, (Map<String, Object>) aChecks, isDebug, isDebugOnError);
    } else {
      throw new IllegalStateException("Illegal entry " + aChecks);
    }
  }

  private HealthRecord check(final String name, Map<String, Object> aChecks,
          final boolean isDebug, final boolean isDebugOnError) {
    HealthRecord.Builder respBuilder = HealthRecord.newBuilder()
            .setName(name)
            .setStatus(HealthStatus.HEALTHY);
    List<HealthRecord> records = new ArrayList<>(aChecks.size());
    for (Map.Entry<String, Object> entry : aChecks.entrySet()) {
      Object value = entry.getValue();
      HealthRecord record;
      if (value instanceof HealthCheck) {
        record = check(entry.getKey(), (HealthCheck) value, isDebug, isDebugOnError);
      } else if (value instanceof Map) {
        record = check(entry.getKey(), (Map<String, Object>) value, isDebug, isDebugOnError);
      } else {
        throw new IllegalStateException("Illegal entry " + entry);
      }
      records.add(record);
      if (record.getStatus() == HealthStatus.UNHEALTHY) {
        respBuilder.setStatus(HealthStatus.UNHEALTHY);
        break;
      }
    }
    respBuilder.setComponentsHealth(records);
    return respBuilder.build();
  }

  private HealthRecord check(final String name,
          final HealthCheck check, final boolean isDebug, final boolean isDebugOnError) {
    try (ExecutionContext ec = ExecutionContexts.start(name,
            check.timeout(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS)) {
      HealthRecord.Builder respBuilder = HealthRecord.newBuilder()
              .setName(name);
      try {
        check.test(LOG);
        respBuilder.setStatus(HealthStatus.HEALTHY);
        if (isDebug) {
          respBuilder.setDetail(ec.getDebugDetail(host, null));
        }
      } catch (Exception ex) {
        respBuilder.setStatus(HealthStatus.UNHEALTHY);
        if (isDebugOnError) {
          respBuilder.setDetail(ec.getDebugDetail(host, ex));
        }
      }
      return respBuilder.build();
    }
  }

}
