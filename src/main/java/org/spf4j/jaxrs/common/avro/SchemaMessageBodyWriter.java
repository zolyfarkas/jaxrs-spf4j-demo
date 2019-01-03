
package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.spf4j.base.Json;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/json", "text/plain"})
public final class SchemaMessageBodyWriter implements MessageBodyWriter<Schema> {

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return Schema.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(Schema t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
          MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
          throws IOException, WebApplicationException {
    t.toJson(Json.FACTORY.createJsonGenerator(entityStream));
  }


}
