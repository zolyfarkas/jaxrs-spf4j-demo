
package org.spf4j.servlet;

import java.util.List;
import org.spf4j.base.ExecutionContext.Tag;
import org.spf4j.log.Level;

/**
 * Custom
 * @author Zoltan Farkas
 */
public class ContextTags {

  public static final Tag<List<Object>> LOG_ATTRIBUTES = new Tag<List<Object>>(){
    @Override
    public String toString() {
      return "LA";
    }
  };

  public static final Tag<Level> LOG_LEVEL = new Tag<Level>(){
    @Override
    public String toString() {
      return "LL";
    }
  };


}
