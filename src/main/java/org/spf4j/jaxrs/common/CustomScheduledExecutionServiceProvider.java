package org.spf4j.jaxrs.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientBackgroundScheduler;
import org.glassfish.jersey.server.BackgroundScheduler;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;
import org.spf4j.concurrent.ContextPropagatingScheduledExecutorService;
import org.spf4j.concurrent.CustomThreadFactory;

/**
 * @author Zoltan Farkas
 */
@Provider
@BackgroundScheduler
@ClientBackgroundScheduler
public class CustomScheduledExecutionServiceProvider implements ScheduledExecutorServiceProvider {

  @Override
  public ScheduledExecutorService getExecutorService() {
    return new ContextPropagatingScheduledExecutorService(new ScheduledThreadPoolExecutor(
                  Integer.getInteger("jersey.scheduler.coreThreads", 2),
                  new CustomThreadFactory("jsched",
                          Boolean.getBoolean("jersey.scheduler.daemon"),
                          Integer.getInteger("jersey.scheduler.priority", Thread.NORM_PRIORITY))));
  }

  @Override
  public void dispose(ExecutorService executorService) {
    CustomExecutorServiceProvider.disposeExecutor(executorService,
            Integer.getInteger("spf4j.jersey.executor.cleanShutdownSeconds", 30));
  }

}
