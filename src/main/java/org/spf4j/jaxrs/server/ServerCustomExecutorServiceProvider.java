package org.spf4j.jaxrs.server;

import org.spf4j.jaxrs.common.executors.*;
import javax.inject.Inject;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ManagedAsyncExecutor;
import org.spf4j.jaxrs.Utils;

/**
 * @author Zoltan Farkas
 */
@Provider
@ManagedAsyncExecutor
public class ServerCustomExecutorServiceProvider extends CustomExecutorServiceProvider {

  @Inject
  public ServerCustomExecutorServiceProvider(final Configuration cfg) {
    super(Utils.getIntConfigValue(cfg, "jersey.server.exec.coreSize", 2),
            Utils.getIntConfigValue(cfg, "jersey.server.exec.maxSize", 256),
            Utils.getIntConfigValue(cfg, "jersey.server.exec.maxIdleMIllis", 120000),
            Utils.getIntConfigValue(cfg, "jersey.server.exec.cleanShutdownWaitMillis", 300000),
            Utils.getStringConfigValue(cfg, "jersey.server.exec.name", "svr-masync"));
  }

}
