package org.spf4j.demo.resources.live;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.spf4j.io.PathsIOException;

/**
 *
 * @author Zoltan Farkas
 */
public class AgedDeletingVisitor implements FileVisitor<Path> {

  private final Instant oldestTime;

  private Path rootFolder;

  private PathsIOException exception;

  private int nrDeleted;

  public AgedDeletingVisitor(long age, TimeUnit unit) {
    this.rootFolder = null;
    this.exception = null;
    this.oldestTime = Instant.now().minusNanos(unit.toNanos(age));
    this.nrDeleted = 0;
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
    if (lastModifiedTime.toInstant().isBefore(oldestTime)) {
      try {
        Files.delete(file);
        nrDeleted++;
      } catch (IOException ex) {
        if (rootFolder == null) {
          throw new PathsIOException(file, ex);
        } else {
          suppress(ex, file);
        }
      }
    }
    return FileVisitResult.CONTINUE;
  }

  private void suppress(final IOException ex, final Path path) {
    if (exception == null) {
      exception = new PathsIOException(path, ex);
    } else {
      exception.add(path, ex);
    }
  }

  @Override
  public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
    if (Files.list(dir).findAny().isEmpty()) {
      try {
        Files.delete(dir);
      } catch (IOException ex) {
        suppress(ex, dir);
      }
      if (exception != null && dir.equals(rootFolder)) {
        throw exception;
      }
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
    if (rootFolder == null) {
      rootFolder = dir;
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
    if (rootFolder == null) {
      throw new PathsIOException(file, exc);
    } else {
      suppress(exc, file);
      return FileVisitResult.CONTINUE;
    }
  }

  public int getNrDeleted() {
    return nrDeleted;
  }

  

}
