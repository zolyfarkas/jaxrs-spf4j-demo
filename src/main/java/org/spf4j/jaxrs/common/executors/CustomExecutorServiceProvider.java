package org.spf4j.jaxrs.common.executors;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.spf4j.base.Threads;
import org.spf4j.concurrent.ContextPropagatingExecutorService;
import org.spf4j.concurrent.LifoThreadPoolBuilder;

/**
 * @author Zoltan Farkas
 */
public class CustomExecutorServiceProvider implements ExecutorServiceProvider {

  private final int coreSize;

  private final int maxSize;

  private final int idleMillis;

  private final int cleanShutdownWaitMillis;

  private final String executorName;

  public CustomExecutorServiceProvider(int coreSize, int maxSize, int idleMillis,
          final int cleanShutdownWaitMillis, String executorName) {
    this.coreSize = coreSize;
    this.maxSize = maxSize;
    this.idleMillis = idleMillis;
    this.cleanShutdownWaitMillis = cleanShutdownWaitMillis;
    this.executorName = executorName;
  }



  @Override
  public ExecutorService getExecutorService() {
    Logger.getLogger(CustomExecutorServiceProvider.class.getName())
            .log(Level.FINE, "Starting executor {0}", executorName);
    return new ContextPropagatingExecutorService(LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(coreSize)
            .withMaxSize(maxSize)
            .withDaemonThreads(true)
            .withMaxIdleTimeMillis(idleMillis)
            .withPoolName(executorName)
            .withQueueSizeLimit(0)
            .enableJmx()
            .build());
  }

  @Override
  public void dispose(final ExecutorService executorService) {
    disposeExecutor(executorService, cleanShutdownWaitMillis);
  }

  public static void disposeExecutor(ExecutorService executorService, final int cleanShutdownWaitMillis) {
    Logger.getLogger(CustomExecutorServiceProvider.class.getName())
            .log(Level.FINE, "Shutting down executor {0}", executorService);
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(cleanShutdownWaitMillis,
              TimeUnit.MILLISECONDS)) {
        List<Runnable> stillRunning = executorService.shutdownNow();
        Logger.getLogger("jerser.executor").log(Level.WARNING,
                "Unable to shutdown cleanly, still running {0}", stillRunning);
        Threads.dumpToPrintStream(System.err);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      List<Runnable> stillRunning = executorService.shutdownNow();
      Logger.getLogger("jersey.executor").log(Level.WARNING,
              "Interrupted during shutdown, stilll running {0}", stillRunning);
    }
  }


}
