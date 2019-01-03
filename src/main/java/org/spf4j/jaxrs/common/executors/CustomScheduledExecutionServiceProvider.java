package org.spf4j.jaxrs.common.executors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;
import org.spf4j.concurrent.ContextPropagatingScheduledExecutorService;
import org.spf4j.concurrent.CustomThreadFactory;

/**
 * @author Zoltan Farkas
 */
public class CustomScheduledExecutionServiceProvider implements ScheduledExecutorServiceProvider {

  private final int coreSize;

  private final String executorName;

  private final int cleanShutdownWaitMillis;

  public CustomScheduledExecutionServiceProvider(int coreSize, int cleanShutdownWaitMillis,  String executorName) {
    this.coreSize = coreSize;
    this.executorName = executorName;
    this.cleanShutdownWaitMillis = cleanShutdownWaitMillis;
  }



  @Override
  public ScheduledExecutorService getExecutorService() {
    Logger.getLogger(CustomScheduledExecutionServiceProvider.class.getName())
            .log(Level.FINE, "Starting executor {0}", executorName);
    return new ContextPropagatingScheduledExecutorService(new ScheduledThreadPoolExecutor(
                  coreSize, new CustomThreadFactory(executorName, false, Thread.NORM_PRIORITY)));
  }

  @Override
  public void dispose(ExecutorService executorService) {
    CustomExecutorServiceProvider.disposeExecutor(executorService, cleanShutdownWaitMillis);
  }

}
