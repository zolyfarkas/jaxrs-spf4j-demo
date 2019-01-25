
package org.spf4j.jaxrs;

import java.io.IOException;

/**
 * @author Zoltan Farkas
 */
public interface ContentStreamWriter<T> {

  void write(T object) throws IOException;

  void flush() throws IOException;
  
}
