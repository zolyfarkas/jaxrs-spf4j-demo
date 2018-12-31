package org.spf4j.jaxrs.server;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Path;
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
import org.spf4j.base.ExecutionContexts;
import org.spf4j.log.ExecContextLogger;

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
    Class<?>[] parameterTypes = method.getParameterTypes();
    int i  = 0;
    for (Class c : parameterTypes) {
      if (AsyncResponse.class.isAssignableFrom(c)) {
        final int loc = i;
        return Arrays.asList(new LoggingInterceptor(method), new AsycResponseTimeoutSetterInterceptor(loc));
      }
      i++;
    }
    return  Collections.singletonList(new LoggingInterceptor(method));
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
      return invocation.proceed();
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

}
