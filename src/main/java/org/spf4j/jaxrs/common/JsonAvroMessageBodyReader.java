package org.spf4j.jaxrs.common;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.ExtendedJsonDecoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.AvroNamesRefResolver;
import org.spf4j.http.Headers;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/json", "text/plain"})
public class JsonAvroMessageBodyReader implements MessageBodyReader<Object> {

  private final SchemaResolver client;

  @Inject
  public JsonAvroMessageBodyReader(final SchemaResolver client) {
    this.client = client;
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return true;
  }

  @Override
  public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations,
          MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
          throws IOException, WebApplicationException {
    String schemaStr = httpHeaders.getFirst(Headers.CONTENT_SCHEMA);
    Schema writer = new Schema.Parser(new AvroNamesRefResolver(client)).parse(schemaStr);
    Schema readerSchema = ReflectData.get().getSchema(genericType);
    DatumReader reader = new SpecificDatumReader(writer, readerSchema);
    Decoder decoder = new ExtendedJsonDecoder(readerSchema, entityStream);
    return reader.read(null, decoder);
  }

}
