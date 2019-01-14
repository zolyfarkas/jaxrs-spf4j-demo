
package org.spf4j.http;

import java.util.List;
import org.spf4j.base.ExecutionContext.Tag;
import org.spf4j.http.HttpWarning;
import org.spf4j.log.Level;
import org.spf4j.servlet.CountingHttpServletRequest;
import org.spf4j.servlet.CountingHttpServletResponse;

/**
 * Custom
 * @author Zoltan Farkas
 */
public class ContextTags {

  /**
   * Additional log attributes (objects) that will be logged in the standard log entry for the context.
   */
  public static final Tag<List<Object>> LOG_ATTRIBUTES = new Tag<List<Object>>(){
    @Override
    public String toString() {
      return "LA";
    }
  };

  /**
   * Http warnings attached to current execution context.
   */
  public static final Tag<List<HttpWarning>> HTTP_WARNINGS =  new Tag<List<HttpWarning>>(){
    @Override
    public String toString() {
      return "HW";
    }
  };

  /**
   * Upgrade the log level of the standard LOG entry for the context.
   */
  public static final Tag<Level> LOG_LEVEL = new Tag<Level>(){
    @Override
    public String toString() {
      return "LL";
    }
  };


  public static final Tag<CountingHttpServletRequest> HTTP_REQ = new Tag<CountingHttpServletRequest>() {
    @Override
    public String toString() {
      return "HREQ";
    }
  };

  public static final Tag<CountingHttpServletResponse> HTTP_RESP = new Tag<CountingHttpServletResponse>() {
    @Override
    public String toString() {
      return "HRESP";
    }
  };    


}
