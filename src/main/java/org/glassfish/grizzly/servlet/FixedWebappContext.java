
package org.glassfish.grizzly.servlet;

import javax.servlet.Servlet;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 *
 * @author Zoltan Farkas
 */
public final class FixedWebappContext extends WebappContext {

  public FixedWebappContext(String displayName) {
    super(displayName);
  }

  public FixedWebappContext(String displayName, String contextPath) {
    super(displayName, contextPath);
  }

  public FixedWebappContext(String displayName, String contextPath, String basePath) {
    super(displayName, contextPath, basePath);
  }

  @Override
  protected Servlet createServletInstance(ServletRegistration registration) throws Exception {
    ServletContainer srvlet = (ServletContainer) super.createServletInstance(registration);
    registration.servlet = srvlet;
    return srvlet;
  }


}
