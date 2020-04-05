
package org.spf4j.demo.resources.live;

import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.CloseableIterable;
import org.spf4j.concurrent.DefaultScheduler;

/**
 * @author Zoltan Farkas
 * for in memory implementations see: https://github.com/google/jimfs
 */
public class FSFileStore implements Closeable, FileStore {

  private static final Logger LOG = LoggerFactory.getLogger(FSFileStore.class);

  private final Path store;

  private final ScheduledFuture<?> scheduleWithFixedDelay;

  public FSFileStore(Path store, long retentionTime, TimeUnit tu) {
    this.store = store;
    scheduleWithFixedDelay = DefaultScheduler.instance().scheduleWithFixedDelay(()-> {
      try {
        AgedDeletingVisitor agedDeletingVisitor = new AgedDeletingVisitor(retentionTime, tu);
        Files.walkFileTree(store, agedDeletingVisitor);
        LOG.info("Cleaned up {} file in {}", agedDeletingVisitor.getNrDeleted(), store);
      } catch (IOException | RuntimeException ex) {
        LOG.error("Failured to cleanup files", ex);
      }
    }, 1, 1, TimeUnit.MINUTES);
  }

  public CloseableIterable<String> list(final String path) throws IOException {
    Stream<Path> walk;
    try {
      walk = Files.list(store.resolve(path));
    } catch (NoSuchFileException ex) {
      return null;
    }
    return CloseableIterable.from(Iterables.transform(() -> walk.iterator(), (p) -> p.getFileName().toString()), walk);
  }

  @Override
  public OutputStream storeFile(String filePath) throws IOException {
    Path file = Path.of(filePath);
    if (file.isAbsolute()) {
      throw new IllegalArgumentException();
    }
    Path parent = file.getParent();
    Path streamFolder = store.resolve(parent).normalize();
    Files.createDirectories(streamFolder);
    return Files.newOutputStream(streamFolder.resolve(file.getFileName()));
  }


  @Override
  @Nullable
  public InputStream readFile(String filePath) throws IOException {
    try {
      return Files.newInputStream(store.resolve(Path.of(filePath).normalize()));
    } catch (NoSuchFileException ex) {
      return null;
    }
  }

  @Override
  @PreDestroy
  public void close() throws IOException {
    scheduleWithFixedDelay.cancel(true);
  }



}
