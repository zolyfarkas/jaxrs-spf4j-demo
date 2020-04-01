/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.demo.resources.live;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.annotation.WillNotClose;
import org.spf4j.base.CloseableIterable;

/**
 * @author Zoltan Farkas
 */
public interface FileStore  extends Closeable {


  @PreDestroy
  void close() throws IOException;

  CloseableIterable<String> list(final String path) throws IOException;

  @Nullable
  InputStream readFile(String path, String fileName) throws IOException;

  void storeFile(String path, String fileName,
          @WillNotClose InputStream is) throws IOException;

  void appendFile(String path, String fileName,
          @WillNotClose InputStream is) throws IOException;


}
