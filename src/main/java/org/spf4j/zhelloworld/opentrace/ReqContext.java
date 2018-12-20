/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.zhelloworld.opentrace;

import io.opentracing.propagation.TextMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

public  final class ReqContext implements TextMap {

    final HttpHeaders headers;

    public ReqContext(final HttpHeaders headers) {
      this.headers = headers;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
      MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
      HashMap<String, String> map = new HashMap<>(requestHeaders.size());
      for (Map.Entry<String, List<String>> entry:requestHeaders.entrySet()) {
        List<String> value = entry.getValue();
        if (!value.isEmpty()) {
          map.put(entry.getKey(), value.get(0));
        }
      }
      return map.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
      throw new UnsupportedOperationException();
    }

  }