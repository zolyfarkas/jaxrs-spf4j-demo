package org.spf4j.jaxrs.common;

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
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ExtendedJsonEncoder;
import org.apache.avro.specific.ExtendedSpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/json", "text/plain"})
public class JsonAvroMessageBodyWriter implements MessageBodyWriter<SpecificRecord> {

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return SpecificRecord.class.isAssignableFrom(type);
  }

  @Override
  public void writeTo(SpecificRecord t, Class<?> type,
          Type genericType, Annotation[] annotations,
          MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
          throws IOException, WebApplicationException {
    Schema schema = t.getSchema();
    String id = schema.getProp("mvnId");
    if (id  == null || id.contains("SNAPSHOT")) {
      httpHeaders.add("Content-Schema", schema);
    } else {
      httpHeaders.add("Content-Schema-Ref", id);
    }
    DatumWriter writer = new ExtendedSpecificDatumWriter(t.getClass());
    Encoder encoder = new ExtendedJsonEncoder(schema, entityStream);
    writer.write(t, encoder);
    encoder.flush();
  }

}
