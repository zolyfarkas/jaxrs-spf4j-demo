package org.spf4j.demo.resources.live;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class FSFileStoreTest {

  @Test(expected = IllegalArgumentException.class)
  public void testFileStore() throws IOException {
    try ( FSFileStore store = new FSFileStore(Files.createTempDirectory("tet"), 10, TimeUnit.SECONDS)) {
      try ( OutputStream storeFile = store.storeFile("../someFile.txt")) {
        storeFile.write(76);
      }
    }
  }

  @Test
  public void testFileStore2() throws IOException {
    FSFileStore store = new FSFileStore(Files.createTempDirectory("test"), 10, TimeUnit.SECONDS);
    try ( OutputStream storeFile = store.storeFile("someFile.txt")) {
      storeFile.write(76);
    }
    try ( InputStream is = store.readFile("someFile.txt")) {
      Assert.assertEquals(76, is.read());
      Assert.assertEquals(-1, is.read());
    }
    store.close();
  }

}
