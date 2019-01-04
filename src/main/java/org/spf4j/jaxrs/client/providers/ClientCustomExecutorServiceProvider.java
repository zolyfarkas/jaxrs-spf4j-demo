package org.spf4j.jaxrs.client.providers;

import org.spf4j.jaxrs.common.executors.*;
import javax.inject.Inject;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.spf4j.jaxrs.Utils;

/**
 * @author Zoltan Farkas
 */
@Provider
@ClientAsyncExecutor
public class ClientCustomExecutorServiceProvider extends CustomExecutorServiceProvider {

  @Inject
  public ClientCustomExecutorServiceProvider(final Configuration cfg) {
    super(Utils.getIntConfigValue(cfg, "jersey.client.exec.coreSize", 1),
            Utils.getIntConfigValue(cfg, "jersey.client.exec.maxSize", 256),
            Utils.getIntConfigValue(cfg, "jersey.client.exec.maxIdleMIllis", 120000),
            Utils.getIntConfigValue(cfg, "jersey.cleanShutdownWaitMillis", 300000),
            Utils.getStringConfigValue(cfg, "jersey.client.exec.name", "clt-masync"));
  }



}
