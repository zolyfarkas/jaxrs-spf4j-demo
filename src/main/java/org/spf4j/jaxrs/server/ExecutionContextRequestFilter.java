
package org.spf4j.jaxrs.server;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import org.spf4j.base.ExecutionContexts;

/**
 * @author Zoltan Farkas
 */
@Provider
@PreMatching
@Priority(1)
public class ExecutionContextRequestFilter implements ContainerRequestFilter  {

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    requestContext.setProperty("xCtx", ExecutionContexts.current());
  }

}
