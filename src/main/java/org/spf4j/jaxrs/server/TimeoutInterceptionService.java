package org.spf4j.jaxrs.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.container.AsyncResponse;
import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InterceptionService;
import org.spf4j.base.ExecutionContexts;

/**
 * Based on  https://blog.dejavu.sk/intercepting-jersey-resource-method-calls/
 * @author Zoltan Farkas
 */
public class TimeoutInterceptionService implements InterceptionService {

  @Override
  public Filter getDescriptorFilter() {
    return new Filter() {
      @Override
      public boolean matches(Descriptor d) {
        return true;
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
        return Collections.singletonList(new MethodInterceptor() {
          @Override
          public Object invoke(MethodInvocation invocation) throws Throwable {
            ((AsyncResponse) invocation.getArguments()[loc])
                    .setTimeout(ExecutionContexts.getTimeToDeadline(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            return invocation.proceed();
          }
        });
      }
      i++;
    }
    return null;
  }

  @Override
  public List<ConstructorInterceptor> getConstructorInterceptors(Constructor<?> constructor) {
    return null;
  }

}
