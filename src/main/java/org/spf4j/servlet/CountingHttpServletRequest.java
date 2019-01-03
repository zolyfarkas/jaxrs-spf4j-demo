
package org.spf4j.servlet;

import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.spf4j.base.Wrapper;

/**
 * @author Zoltan Farkas
 */
public final class CountingHttpServletRequest extends HttpServletRequestWrapper
        implements Wrapper<HttpServletRequest> {

  private CountingServletInputStream is;

  public CountingHttpServletRequest(final HttpServletRequest request) {
    super(request);
  }

  @Override
  public synchronized ServletInputStream getInputStream() throws IOException {
    if (is == null) {
      is = new CountingServletInputStream(super.getInputStream());
    }
    return is;
  }

  public synchronized long getBytesRead() {
    return is == null ? 0 : is.getCount();
  }

  @Override
  public String toString() {
    return "CountingHttpServletRequest{" + "is=" + is + '}';
  }

  @Override
  public HttpServletRequest getWrapped() {
    return (HttpServletRequest) super.getRequest();
  }



}
