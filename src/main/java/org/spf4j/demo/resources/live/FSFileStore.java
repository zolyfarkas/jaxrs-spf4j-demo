
package org.spf4j.demo.resources.live;

import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.WillNotClose;
import org.spf4j.base.CloseableIterable;
import org.spf4j.io.DeletingVisitor;
import org.spf4j.io.Streams;

/**
 * @author Zoltan Farkas
 * for in memory implementations see: https://github.com/google/jimfs
 */
public class FSFileStore implements Closeable, FileStore {

  private final Path store;

  public FSFileStore() throws IOException {
    this(Files.createTempDirectory("hlsStore"));
  }

  public FSFileStore(Path store) {
    this.store = store;
  }

  public CloseableIterable<String> list(final String path) throws IOException {
    Stream<Path> walk = Files.list(store);
    return CloseableIterable.from(Iterables.transform(() -> walk.iterator(), (p) -> p.getFileName().toString()), walk);
  }

  @Override
  public void storeFile(String path, String fileName,
          @WillNotClose InputStream is) throws IOException {
    Path streamFolder = store.resolve(path);
    Files.createDirectories(streamFolder);
    try (OutputStream bos = Files.newOutputStream(streamFolder.resolve(fileName))) {
      Streams.copy(is, bos);
    }
  }

  @Override
  public void appendFile(String path, String fileName,
          @WillNotClose InputStream is) throws IOException {
    Path streamFolder = store.resolve(path);
    Files.createDirectories(streamFolder);
    try (OutputStream bos = Files.newOutputStream(streamFolder.resolve(fileName),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      Streams.copy(is, bos);
    }
  }

  @Override
  @Nullable
  public InputStream readFile(String path, String fileName) throws IOException {
    try {
      return Files.newInputStream(store.resolve(path).resolve(fileName));
    } catch (NoSuchFileException ex) {
      return null;
    }
  }


  @Override
  @PreDestroy
  public void close() throws IOException {
    Files.walkFileTree(store, new DeletingVisitor());
  }



}
