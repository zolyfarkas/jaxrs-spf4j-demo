package org.spf4j.jaxrs.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.Throwables;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.log.Level;
import org.spf4j.log.LogAttribute;
import org.spf4j.log.LogUtils;
import org.spf4j.log.Slf4jLogRecord;
import org.spf4j.servlet.ContextTags;

/**
 * @author Zoltan Farkas
 */
@Provider
public final class LoggingExceptionMapper implements ExceptionMapper<Exception> {

  private final ContainerRequestContext jaxCtx;

  public LoggingExceptionMapper(
          @Context ContainerRequestContext jaxCtx) {
    this.jaxCtx = jaxCtx;
  }

  @Override
  public Response toResponse(final Exception exception) {
    WebApplicationException wex = Throwables.first(exception, WebApplicationException.class);
    int status;
    if (wex != null) {
      Response response = wex.getResponse();
      Object entity = response.getEntity();
      if (entity != null) {
        return response;
      }
      status = response.getStatus();
    } else {
      status = 500;
    }
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx == null) { // Exception mapper can execute in a timeout thread, where context is not available.
      try {
          ctx = (ExecutionContext) jaxCtx.getProperty("xCtx");
      } catch (RuntimeException ex) {
        // THis happens when request already recycled?
        // null at HttpServletRequestImpl.java:253)[grizzly-http-servlet-2.4.0.jar:2.4.0]
        ex.addSuppressed(exception);
        Logger.getLogger("handling.error").log(java.util.logging.Level.SEVERE, "No request context available anymore",
                ex);
        return Response.serverError().entity("Context error, see server logs for detail.").build();
      }
    }
    if (status >= 500) {
      ctx.put(ContextTags.LOG_LEVEL, Level.ERROR);
    }
    ctx.add(ContextTags.LOG_ATTRIBUTES, exception);
    List<Slf4jLogRecord> ctxLogs = new ArrayList();
    ctx.streamLogs((log) -> {
      if (!log.isLogged()) {
        ctxLogs.add(log);
      }
    });
    Collections.sort(ctxLogs, Slf4jLogRecord::compareByTimestamp);
    Logger logger = Logger.getLogger("debug.on.error");
    for (Slf4jLogRecord log : ctxLogs) {
      LogUtils.logUpgrade(logger, org.spf4j.log.Level.INFO, "Detail on Error", LogAttribute.log(log));
      log.setIsLogged();
    }
    ServiceError se = ServiceError.newBuilder()
            .setCode(status)
            .setDetail(new DebugDetail(jaxCtx.getUriInfo().getRequestUri().toString(),
                    Converters.convert(ctx.getId().toString(), ctxLogs), Converters.convert(exception)))
            .setType(exception.getClass().getName())
            .setMessage(exception.getMessage())
            .build();
    return Response.status(500).entity(se).build();
  }

}
