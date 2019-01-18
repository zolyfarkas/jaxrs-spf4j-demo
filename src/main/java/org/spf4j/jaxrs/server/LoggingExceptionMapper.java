package org.spf4j.jaxrs.server;

import java.io.InputStream;
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
import org.spf4j.base.StackSamples;
import org.spf4j.base.Throwables;
import org.spf4j.base.Methods;
import org.spf4j.base.avro.Converters;
import org.spf4j.base.avro.DebugDetail;
import org.spf4j.base.avro.ServiceError;
import org.spf4j.base.avro.StackSampleElement;
import org.spf4j.log.Level;
import org.spf4j.log.Slf4jLogRecord;
import org.spf4j.http.ContextTags;
import org.spf4j.ssdump2.Converter;

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
    String message = exception.getMessage();
    if (message == null) {
      message = "";
    }
    int status;
    Object payload;
    if (wex != null) {
      Response response = wex.getResponse();
      status = response.getStatus();
      if (response.hasEntity()) {
        payload = response.getEntity();
        if (payload instanceof InputStream) {
          payload = null;
        }
      } else {
        payload = null;
      }
    } else {
      status = 500;
      payload = null;
    }
    ExecutionContext ctx = ExecutionContexts.current();
    if (ctx == null) { // Exception mapper can execute in a timeout thread, where context is not available,
      Logger.getLogger("handling.error")
              .log(java.util.logging.Level.WARNING, "No request context available", exception);
      ServiceError.Builder errBuilder = ServiceError.newBuilder()
              .setCode(status)
              .setDetail(new DebugDetail(host,
                      Collections.EMPTY_LIST, Converters.convert(exception), Collections.EMPTY_LIST))
              .setType(exception.getClass().getName())
              .setMessage(message).setPayload(payload);
      return Response.serverError()
              .entity(errBuilder.build())
              .type(MediaType.APPLICATION_JSON_TYPE)
              .build();
    }
    if (status >= 500) {
      ctx.putToRootParent(ContextTags.LOG_LEVEL, Level.ERROR);
    }
    ctx.addToRootParent(ContextTags.LOG_ATTRIBUTES, exception);
    List<Slf4jLogRecord> ctxLogs = new ArrayList<>();
    ExecutionContext curr = ctx;
    while (curr != null) {
      curr.streamLogs((log) -> {
        ctxLogs.add(log);
      });
      curr = curr.getSource();
    }
    Collections.sort(ctxLogs, Slf4jLogRecord::compareByTimestamp);
    StackSamples ss = ctx.getAndClearStackSamples();
    List<StackSampleElement> sses;
    if (ss == null) {
      sses = Collections.EMPTY_LIST;
    } else {
      sses = new ArrayList<>(64);
      Converter.convert(Methods.ROOT, ss, -1, 0, (a, b) -> sses.add(a));
    }
    String ctxId = ctx.getId().toString();
    ServiceError.Builder seBuilder = ServiceError.newBuilder()
            .setCode(status)
            .setDetail(new DebugDetail(host + '/' + ctx.getName(),
                    Converters.convert("", ctxId, ctxLogs),
                    Converters.convert(exception), sses))
            .setType(exception.getClass().getName())
            .setMessage(message).setPayload(payload);
    return Response.status(status).entity(seBuilder.build())
            .build();
  }

}
