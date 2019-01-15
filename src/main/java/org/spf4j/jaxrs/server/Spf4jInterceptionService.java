package org.spf4j.jaxrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InterceptionService;
import org.jvnet.hk2.internal.SystemDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.http.ContextTags;
import org.spf4j.http.Headers;
import org.spf4j.http.HttpWarning;
import org.spf4j.log.ExecContextLogger;
import org.spf4j.servlet.CountingHttpServletResponse;

/**
 * Based on  https://blog.dejavu.sk/intercepting-jersey-resource-method-calls/
 * @author Zoltan Farkas
 */
public class Spf4jInterceptionService implements InterceptionService {

  @Override
  public Filter getDescriptorFilter() {
    return new Filter() {
      @Override
      public boolean matches(Descriptor d) {
        if (d instanceof SystemDescriptor) {
          Annotation path = ((SystemDescriptor) d).getImplementationClass().getAnnotation(Path.class);
          if (path != null) {
            return true;
          } else {
            return false;
          }
        }
        return false;
      }
    };
  }

  @Override
  public List<MethodInterceptor> getMethodInterceptors(Method method) {
    int i  = 0;
    Deprecated dannot = method.getAnnotation(Deprecated.class);
    List<HttpWarning> warnings = null;
    if (dannot != null) {
      warnings = new ArrayList<>(2);
      warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent" ,
              "Deprecated-Operation: " + method.toString()));
    }
    List<MethodInterceptor> extra = null;
    for (Parameter param : method.getParameters()) {
      if (AsyncResponse.class.isAssignableFrom(param.getType())) {
        if (extra == null) {
          extra = new ArrayList<>(2);
        }
        extra.add(new AsycResponseTimeoutSetterInterceptor(i));
        continue;
      }
      Deprecated dp = param.getAnnotation(Deprecated.class);
      if (dp != null) {
        if (warnings == null) {
          warnings = new ArrayList<>(2);
        }
        QueryParam qp = param.getAnnotation(QueryParam.class);
        if (qp != null) {
          warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent" ,
                  "Deprecated-Query-Param: " + qp.value()));
        } else {
          PathParam pp = param.getAnnotation(PathParam.class);
          if (pp != null) {
            warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent" ,
                  "Deprecated-Path-Param: " + pp.value()));
          } else {
            warnings.add(new HttpWarning(HttpWarning.MISCELLANEOUS, "todo_agent" ,
                     "Deprecated-Param: " + param.getName()));
          }
        }
      }
      i++;
    }
    if (warnings != null) {
      if (extra == null) {
        extra = new ArrayList<>(2);
      }
      extra.add(new WarningsInterceptor(warnings));
    }
    if (extra == null) {
      return  Collections.singletonList(new LoggingInterceptor(method));
    } else {
      extra.add(0, new LoggingInterceptor(method));
      return extra;
    }
  }

  @Override
  public List<ConstructorInterceptor> getConstructorInterceptors(Constructor<?> constructor) {
    return null;
  }

  private static class LoggingInterceptor implements MethodInterceptor {

    private final Logger log;

    public LoggingInterceptor(Method m) {
      this.log = new ExecContextLogger(LoggerFactory.getLogger(m.getDeclaringClass().getName() + "->" + m.getName()));
    }


    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      log.debug("invoking", invocation.getArguments());
      Object result = invocation.proceed();
      log.debug("returning", result);
      return result;
    }

  }


  private static class AsycResponseTimeoutSetterInterceptor implements MethodInterceptor {

    private final int loc;

    public AsycResponseTimeoutSetterInterceptor(int loc) {
      this.loc = loc;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      ((AsyncResponse) invocation.getArguments()[loc])
              .setTimeout(ExecutionContexts.getTimeToDeadline(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
      return invocation.proceed();
    }
  }

  private static class WarningsInterceptor implements MethodInterceptor {

    private final List<HttpWarning> warnings;

    public WarningsInterceptor(List<HttpWarning> warnings) {
      this.warnings = warnings;
    }

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
      ExecutionContext current = ExecutionContexts.current();
      CountingHttpServletResponse resp = current.get(ContextTags.HTTP_RESP);
      for (HttpWarning warning: warnings) {
        current.add(ContextTags.HTTP_WARNINGS, warning);
        // TODO, encode warning string.
        // https://docs.oracle.com/javaee/6/api/javax/mail/internet/MimeUtility.html
        resp.addHeader(Headers.WARNING, warning.toString());
      }
      return invocation.proceed();
    }

  }

}
