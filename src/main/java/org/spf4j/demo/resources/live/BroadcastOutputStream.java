
package org.spf4j.demo.resources.live;

import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.spf4j.base.Closeables;

/**
 * @author Zoltan Farkas
 */
public class BroadcastOutputStream extends OutputStream {

  private final List<OutputStream> streams;

  private final List<Closeable> closeables;

  public BroadcastOutputStream(final OutputStream os) {
    streams = new ArrayList<>(4);
    closeables = new ArrayList<>(4);
    streams.add(os);
  }

  public void addStream(final OutputStream os) {
    streams.add(os);
  }

  public void addCloseable(final Closeable cls) {
    closeables.add(cls);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    for (OutputStream os: streams) {
      os.write(b, off, len);
    }
  }

  @Override
  public void write(byte[] b) throws IOException {
    for (OutputStream os: streams) {
      os.write(b);
    }
  }

  @Override
  public void write(int arg0) throws IOException {
    for (OutputStream os: streams) {
      os.write(arg0);
    }
  }

  @Override
  public void close() throws IOException {
    Exception ex = Closeables.closeAll(Iterables.concat(streams, closeables));
    if (ex != null) {
      if (ex instanceof IOException) {
        throw (IOException) ex;
      } else {
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public void flush() throws IOException {
    for (OutputStream os: streams) {
      os.flush();
    }
  }



}
