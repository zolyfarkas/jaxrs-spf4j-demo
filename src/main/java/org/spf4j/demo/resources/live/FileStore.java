
package org.spf4j.demo.resources.live;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import org.glassfish.jersey.spi.Contract;
import org.spf4j.base.CloseableIterable;

/**
 * @author Zoltan Farkas
 */
@Contract
public interface FileStore  extends Closeable {


  @PreDestroy
  void close() throws IOException;

  @Nullable
  CloseableIterable<String> list(final String path) throws IOException;

  @Nullable
  InputStream readFile(String filePath) throws IOException;

  OutputStream storeFile(String filePath) throws IOException, TimeoutException;


}
