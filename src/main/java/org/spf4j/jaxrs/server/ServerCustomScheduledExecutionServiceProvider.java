package org.spf4j.jaxrs.server;

import org.spf4j.jaxrs.common.executors.*;
import javax.inject.Inject;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.BackgroundScheduler;
import org.spf4j.jaxrs.Utils;

/**
 * @author Zoltan Farkas
 */
@Provider
@BackgroundScheduler
public class ServerCustomScheduledExecutionServiceProvider extends CustomScheduledExecutionServiceProvider {

  @Inject
  public ServerCustomScheduledExecutionServiceProvider(final Configuration cfg) {
    super(Utils.getIntConfigValue(cfg, "jersey.server.sched_exec.coreSize", 2),
            Utils.getIntConfigValue(cfg, "jersey.server.ched_exec.cleanShutdownWaitMillis", 300000),
            Utils.getStringConfigValue(cfg, "jersey.server.ched_exec.name", "svr-bsched"));
  }

}
