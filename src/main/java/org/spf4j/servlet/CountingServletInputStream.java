package org.spf4j.servlet;

import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import org.spf4j.base.Wrapper;

/**
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class CountingServletInputStream extends ServletInputStream
  implements Wrapper<ServletInputStream> {

  private final ServletInputStream in;

  private long count;

  private long mark = -1;

  public CountingServletInputStream(final ServletInputStream wrapped) {
    this.in = wrapped;
    this.count = 0;
  }

  @Override
  public boolean isFinished() {
     return in.isFinished();
  }

  @Override
  public boolean isReady() {
    return in.isReady();
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    in.setReadListener(readListener);
  }

  /** Returns the number of bytes read. */
  public synchronized long getCount() {
    return count;
  }

  @Override
  public synchronized int read() throws IOException {
    int result = in.read();
    if (result != -1) {
      count++;
    }
    return result;
  }

  @Override
  public  synchronized int read(final byte[] b, final int off, final int len) throws IOException {
    int result = in.read(b, off, len);
    if (result != -1) {
      count += result;
    }
    return result;
  }

  @Override
  public synchronized long skip(final long n) throws IOException {
    long result = in.skip(n);
    count += result;
    return result;
  }

  @Override
  public synchronized void mark(final int readlimit) {
    in.mark(readlimit);
    mark = count;
    // it's okay to mark even if mark isn't supported, as reset won't work
  }

  @Override
  public synchronized void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("Mark not supported by " + in);
    }
    if (mark == -1) {
      throw new IOException("Mark not set for " + this);
    }

    in.reset();
    count = mark;
  }

  public ServletInputStream getWrappedStream() {
    return in;
  }

  public synchronized long getMark() {
    return mark;
  }

  @Override
  public boolean markSupported() {
    return in.markSupported();
  }

  @Override
  public int available() throws IOException {
    return in.available();
  }


  @Override
  public void close() throws IOException {
    in.close();
  }

  @Override
  public synchronized String toString() {
    return "CountingServletInputStream{" + "in=" + in + ", count=" + count + ", mark=" + mark + '}';
  }

  @Override
  public ServletInputStream getWrapped() {
    return this.in;
  }


}
