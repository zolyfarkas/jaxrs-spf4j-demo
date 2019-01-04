package org.spf4j.jaxrs.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.ResponseErrorMapper;
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
public final class LoggingExceptionMapper implements ExceptionMapper<Throwable>, ResponseErrorMapper {

  private final String host;

  public LoggingExceptionMapper() {
    String h;
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      h = localHost.getHostName();
    } catch (UnknownHostException ex) {
      Logger.getLogger(LoggingExceptionMapper.class.getName())
              .log(java.util.logging.Level.WARNING, "Unable to figura out host name", ex);
      h = "unknown";
    }
    host = h;
  }

  @Override
  public Response toResponse(final Throwable exception) {
    WebApplicationException wex = Throwables.first(exception, WebApplicationException.class);
    int status;
    if (wex != null) {
      Response response = wex.getResponse();
      if (response.hasEntity()) {
         response.getEntity();
         return response;
      }
      status = response.getStatus();
    } else {
      status = 500;
    }
    ExecutionContext ctx = ExecutionContexts.current();
    String message = exception.getMessage();
    if (message == null) {
      message = "";
    }
    if (ctx == null) { // Exception mapper can execute in a timeout thread, where context is not available,
        Logger.getLogger("handling.error")
                .log(java.util.logging.Level.WARNING, "No request context available", exception);
        return Response.serverError()
                .entity(ServiceError.newBuilder()
            .setCode(status)
            .setDetail(new DebugDetail(host,
                    Collections.EMPTY_LIST, Converters.convert(exception)))
            .setType(exception.getClass().getName())
            .setMessage(message)
            .build())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
    if (status >= 500) {
      ctx.putToRoot(ContextTags.LOG_LEVEL, Level.ERROR);
    }
    ctx.addToRoot(ContextTags.LOG_ATTRIBUTES, exception);
    List<Slf4jLogRecord> ctxLogs = new ArrayList<>();
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
            .setDetail(new DebugDetail(host + '/' + ctx.getName(),
                    Converters.convert("", ctx.getId().toString(), ctxLogs), Converters.convert(exception)))
            .setType(exception.getClass().getName())
            .setMessage(message)
            .build();
    return Response.status(status).entity(se)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build();
  }

}
