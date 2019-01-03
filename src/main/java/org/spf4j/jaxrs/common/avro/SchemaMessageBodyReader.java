
package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/json", "text/plain"})
public final class SchemaMessageBodyReader implements MessageBodyReader<Schema> {

  @Override
  public boolean isReadable(final Class<?> type, final Type genericType,
          final Annotation[] annotations, final MediaType mediaType) {
    return Schema.class.isAssignableFrom(type);
  }

  @Override
  public Schema readFrom(final Class<Schema> type, final Type genericType, final Annotation[] annotations,
          final MediaType mediaType, final MultivaluedMap<String, String> httpHeaders, final InputStream entityStream)
          throws IOException, WebApplicationException {
    return new Schema.Parser().parse(entityStream);
  }

}
