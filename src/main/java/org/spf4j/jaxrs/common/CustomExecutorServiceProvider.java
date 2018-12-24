package org.spf4j.jaxrs.common;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.client.ClientAsyncExecutor;
import org.glassfish.jersey.server.ManagedAsyncExecutor;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.spf4j.concurrent.ContextPropagatingExecutorService;
import org.spf4j.concurrent.LifoThreadPool;
import org.spf4j.concurrent.LifoThreadPoolBuilder;

/**
 * @author Zoltan Farkas
 */
@Provider
@ManagedAsyncExecutor
@ClientAsyncExecutor
public class CustomExecutorServiceProvider implements ExecutorServiceProvider {

  @Override
  public ExecutorService getExecutorService() {
    LifoThreadPool ltp = LifoThreadPoolBuilder.newBuilder()
            .withCoreSize(Integer.getInteger("spf4j.jersey.executor.coreSize", 2))
            .withMaxSize(Integer.getInteger("spf4j.jersey.executor.maxSize", 8))
            .withDaemonThreads(true)
            .withMaxIdleTimeMillis(Integer.getInteger("spf4j.jersey.executor.idleMillis", 120000))
            .withPoolName("jy-core")
            .withQueueSizeLimit(0)
            .enableJmx()
            .build();

    return new ContextPropagatingExecutorService(ltp);
  }

  @Override
  public void dispose(final ExecutorService executorService) {
    disposeExecutor(executorService, Integer.getInteger("spf4j.jersey.executor.cleanShutdownSeconds", 30));
  }

  public static void disposeExecutor(ExecutorService executorService, final int cleanWaitSeconds) {
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(cleanWaitSeconds,
              TimeUnit.SECONDS)) {
        List<Runnable> stillRunning = executorService.shutdownNow();
        Logger.getLogger("jerser.executor").log(Level.WARNING,
                "Unsable to shutdown cleanly, stilll running {0}", stillRunning);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      List<Runnable> stillRunning = executorService.shutdownNow();
      Logger.getLogger("jerser.executor").log(Level.WARNING,
              "Interrupted during shutdown, stilll running {0}", stillRunning);
    }
  }


}
