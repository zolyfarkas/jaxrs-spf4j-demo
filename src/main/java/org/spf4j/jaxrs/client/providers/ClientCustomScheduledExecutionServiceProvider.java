package org.spf4j.jaxrs.client.providers;

import org.spf4j.jaxrs.common.executors.*;
import javax.inject.Inject;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientBackgroundScheduler;
import org.spf4j.jaxrs.Utils;

/**
 * @author Zoltan Farkas
 */
@Provider
@ClientBackgroundScheduler
public class ClientCustomScheduledExecutionServiceProvider extends CustomScheduledExecutionServiceProvider {

  @Inject
  public ClientCustomScheduledExecutionServiceProvider(final Configuration cfg) {
    super(Utils.getIntConfigValue(cfg, "jersey.client.sched_exec.coreSize", 1),
            Utils.getIntConfigValue(cfg, "jersey.cleanShutdownWaitMillis", 300000),
            Utils.getStringConfigValue(cfg, "jersey.client.ched_exec.name", "clt-bsched"));
  }

}
