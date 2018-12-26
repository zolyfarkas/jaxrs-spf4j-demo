
package org.spf4j.servlet;

import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import org.spf4j.base.Wrapper;

/**
 *
 * @author Zoltan Farkas
 */
@ParametersAreNonnullByDefault
public final class CountingServletOutputStream extends ServletOutputStream
         implements Wrapper<ServletOutputStream> {

  private final ServletOutputStream out;

  private long count;


  public CountingServletOutputStream(final ServletOutputStream out) {
    this.out = out;
  }

  @Override
  public boolean isReady() {
    return out.isReady();
  }

  @Override
  public void setWriteListener(final WriteListener writeListener) {
    out.setWriteListener(writeListener);
  }

  /** Returns the number of bytes written. */
  public synchronized long getCount() {
    return count;
  }

  @Override
  public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
    out.write(b, off, len);
    count += len;
  }

  @Override
  public synchronized void write(final int b) throws IOException {
    out.write(b);
    count++;
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  @Override
  public String toString() {
    return "CountingServletOutputStream{" + "out=" + out + ", count=" + count + '}';
  }

  @Override
  public ServletOutputStream getWrapped() {
    return this.out;
  }

}
