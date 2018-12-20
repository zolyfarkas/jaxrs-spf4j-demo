
package org.spf4j.jaxrs.server;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;

/**
 * A provider to allow injecting, ExecutionContexts.
 * @author Zoltan Farkas
 */
@Provider
public class ExecutionContextResolver implements ContextResolver<ExecutionContext> {

  @Override
  public ExecutionContext getContext(final Class<?> type) {
    if (type == ExecutionContext.class) {
      return ExecutionContexts.current();
    }
    return null;
  }

}
