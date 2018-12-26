
package org.spf4j.servlet;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.spf4j.base.Wrapper;

/**
 *
 * @author Zoltan Farkas
 */
public final class CountingHttpServletResponse extends HttpServletResponseWrapper
  implements Wrapper<HttpServletResponse> {

  private CountingServletOutputStream os;

  public CountingHttpServletResponse(final HttpServletResponse response) {
    super(response);
  }

  @Override
  public synchronized ServletOutputStream getOutputStream() throws IOException {
    if (os == null) {
      os = new CountingServletOutputStream(super.getOutputStream());
    }
    return os;
  }

  public synchronized long getBytesWritten() {
    return os == null ? 0 : os.getCount();
  }

  @Override
  public String toString() {
    return "CountingHttpServletResponse{" + "os=" + os + '}';
  }

  @Override
  public HttpServletResponse getWrapped() {
    return (HttpServletResponse) super.getResponse();
  }

}
