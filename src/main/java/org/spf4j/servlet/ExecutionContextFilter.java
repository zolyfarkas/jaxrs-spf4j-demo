package org.spf4j.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.SysExits;
import org.spf4j.base.Throwables;
import org.spf4j.base.TimeSource;
import org.spf4j.http.Headers;
import org.spf4j.log.Level;
import org.spf4j.log.LogAttribute;
import org.spf4j.log.LogUtils;
import org.spf4j.log.Slf4jLogRecord;

/**
 * A Filter for REST services
 *
 * HTTP headers:
 *
 * request-deadline → SecondsSinceEpoch Nanos? requestt-timeout → TimeoutValue TimeoutUnit? TimeoutValue → {positive
 * integer as ASCII string of at most 8 digits} TimeoutUnit → Hour / Minute / Second / Millisecond / Microsecond /
 * Nanosecond Hour → "H" Minute → "M" Second → "S" Millisecond → "m" Microsecond → "u" Nanosecond → "n"
 */
@WebFilter(asyncSupported = true)
public class ExecutionContextFilter implements Filter {

  public static final String EXECUTION_CONTEXT_SERVLET_PROPERTY = "srvEc";

  public static final String CFG_MAX_TIMEOUT_NANOS = "spf4j.jax-rs.maxTimeoutNanos";

  public static final String CFG_DEFAULT_TIMEOUT_NANOS = "spf4j.jax-rs.defaultTimeoutNanos";

  public static final String CFG_DEADLINE_HEADER_NAME = "spf4j.jax-rs.deadlineHeaderName";

  public static final String CFG_TIMEOUT_HEADER_NAME = "spf4j.jax-rs.timeoutHeaderName";

  public static final String CFG_ID_HEADER_NAME = "spf4j.jax-rs.idHeaderName";

  private long maxTimeoutNanos;

  private long defaultTimeoutNanos;

  private String deadlineHeaderName;

  private String timeoutHeaderName;

  private String idHeaderName;

  private Logger log;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    log = Logger.getLogger("org.spf4j.servlet." + filterConfig.getFilterName());
    maxTimeoutNanos = Filters.getLongParameter(filterConfig, CFG_MAX_TIMEOUT_NANOS, TimeUnit.MINUTES.toNanos(10));
    defaultTimeoutNanos = Filters.getLongParameter(filterConfig, CFG_DEFAULT_TIMEOUT_NANOS, TimeUnit.MINUTES.toNanos(1));
    if (defaultTimeoutNanos > maxTimeoutNanos) {
      throw new ServletException("Invalid server configuration,"
              + " default timeout must be smaller than max timeout " + defaultTimeoutNanos + " < " + maxTimeoutNanos);
    }
    deadlineHeaderName = Filters.getStringParameter(filterConfig, CFG_DEADLINE_HEADER_NAME, Headers.REQ_DEADLINE);
    timeoutHeaderName = Filters.getStringParameter(filterConfig, CFG_TIMEOUT_HEADER_NAME, Headers.REQ_TIMEOUT);
    idHeaderName = Filters.getStringParameter(filterConfig, CFG_ID_HEADER_NAME, Headers.REQ_ID);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
          throws IOException, ServletException {

    CountingHttpServletRequest httpReq = new CountingHttpServletRequest((HttpServletRequest) request);
    CountingHttpServletResponse httpResp = new CountingHttpServletResponse((HttpServletResponse) response);
    long startTimeNanos = TimeSource.nanoTime();
    String deadlineStr = httpReq.getHeader(deadlineHeaderName);
    long deadlineNanos;
    if (deadlineStr == null) {
      String timeoutStr = httpReq.getHeader(timeoutHeaderName);
      if (timeoutStr == null) {
        deadlineNanos = startTimeNanos + defaultTimeoutNanos;
      } else {
        deadlineNanos = startTimeNanos + ProtocolTimeUnit.parseTimeoutNanos(timeoutStr);
      }
    } else {
      deadlineNanos = ProtocolTimeUnit.parseDeadlineNanos(deadlineStr);
    }
    ExecutionContext ctx = ExecutionContexts.start(httpReq.getMethod() + ' ' + httpReq.getPathInfo(),
            httpReq.getHeader(idHeaderName), null, startTimeNanos, deadlineNanos);
    ctx.put(ContextTags.HTTP_REQ, httpReq);
    ctx.put(ContextTags.HTTP_RESP, httpResp);
    request.setAttribute(EXECUTION_CONTEXT_SERVLET_PROPERTY, ctx);

    try {
      chain.doFilter(httpReq, httpResp);
      if (request.isAsyncStarted()) {
        ctx.detach();
        AsyncContext asyncContext = request.getAsyncContext();
        asyncContext.setTimeout(ctx.getMillisToDeadline());
        asyncContext.addListener(new AsyncListener() {
          @Override
          public void onComplete(final AsyncEvent event) throws IOException {
            logRequestEnd(org.spf4j.log.Level.INFO, ctx, httpReq.getBytesRead(), httpResp.getBytesWritten());
            ctx.close();
          }

          @Override
          public void onTimeout(final AsyncEvent event) throws IOException {
          }

          @Override
          public void onError(final AsyncEvent event) throws IOException {
          }

          @Override
          public void onStartAsync(final AsyncEvent event) throws IOException {
          }
        }, request, response);
      } else {
        logRequestEnd(org.spf4j.log.Level.INFO, ctx, httpReq.getBytesRead(), httpResp.getBytesWritten());
        ctx.close();
      }
    } catch (Throwable t) {
      if (Throwables.isNonRecoverable(t)) {
        org.spf4j.base.Runtime.goDownWithError(t, SysExits.EX_SOFTWARE);
      }
      logContextLogs(ctx);
      logRequestEnd(org.spf4j.log.Level.ERROR, ctx, httpReq.getBytesRead(), httpResp.getBytesWritten());
    }
  }

  public void logRequestEnd(final Level plevel, final ExecutionContext ctx,
          final long contentBytesRead, final long contentBytesWritten) {
    logRequestEnd(log, plevel, ctx, contentBytesRead, contentBytesWritten);
  }

  public static void logRequestEnd(final Logger logger, final Level plevel,
          final ExecutionContext ctx, final long contentBytesRead, final long contentBytesWritten) {
    org.spf4j.log.Level level;
    org.spf4j.log.Level ctxOverride = ctx.get(ContextTags.LOG_LEVEL);
    if (ctxOverride != null && ctxOverride.ordinal() > plevel.ordinal()) {
      level = ctxOverride;
    } else {
      level = plevel;
    }
    Object[] args;
    List<Object> logAttrs = ctx.get(ContextTags.LOG_ATTRIBUTES);
    if (logAttrs == null || logAttrs.isEmpty()) {
      args = new Object[]{ctx.getName(),
        LogAttribute.traceId(ctx.getId()),
        LogAttribute.execTimeMicros(TimeSource.nanoTime() - ctx.getStartTimeNanos(), TimeUnit.NANOSECONDS),
        LogAttribute.value("inBytes", contentBytesRead), LogAttribute.value("outBytes", contentBytesWritten)
      };
    } else {
      args = new Object[5 + logAttrs.size()];
      args[0] = ctx.getName();
      args[1] = LogAttribute.traceId(ctx.getId());
      args[2] = LogAttribute.execTimeMicros(TimeSource.nanoTime() - ctx.getStartTimeNanos(), TimeUnit.NANOSECONDS);
      args[3] = LogAttribute.value("inBytes", contentBytesRead);
      args[4] = LogAttribute.value("outBytes", contentBytesWritten);
      int i = 5;
      for (Object obj : logAttrs) {
        args[i++] = obj;
      }
    }
    logger.log(level.getJulLevel(), "Done {0}", args);
  }

  private void logContextLogs(final ExecutionContext ctx) {
    List<Slf4jLogRecord> ctxLogs = new ArrayList<>();
    ctx.streamLogs((log) -> {
      if (!log.isLogged()) {
        ctxLogs.add(log);
      }
    });
    Collections.sort(ctxLogs, Slf4jLogRecord::compareByTimestamp);
    for (Slf4jLogRecord log : ctxLogs) {
      LogUtils.logUpgrade(this.log, org.spf4j.log.Level.INFO, "Detail on Error", LogAttribute.log(log));
    }
  }

  @Override
  public void destroy() {
    // nothing to destroy
  }

}
