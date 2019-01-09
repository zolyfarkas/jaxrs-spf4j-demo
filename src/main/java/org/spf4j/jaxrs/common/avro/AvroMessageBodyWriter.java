
package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ExtendedReflectData;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.codehaus.jackson.JsonGenerator;
import org.spf4j.base.Json;
import org.spf4j.http.Headers;

/**
 * @author Zoltan Farkas
 */
public abstract class AvroMessageBodyWriter implements MessageBodyWriter<Object> {


  private final SchemaResolver client;

  @Inject
  public AvroMessageBodyWriter(final SchemaResolver client) {
    this.client = client;
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return true;
  }

  public abstract Encoder getEncoder(final Schema writerSchema, final OutputStream os)
          throws IOException;


  @Override
  public void writeTo(Object t, Class<?> type,
          Type genericType, Annotation[] annotations,
          MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
          throws IOException, WebApplicationException {
    Schema schema = ExtendedReflectData.get().getSchema(genericType != null ? genericType : type);
    if (schema == null) {
      schema = ExtendedReflectData.get().createSchema(genericType != null ? genericType : type, t, new HashMap<>());
    }
    String id = schema.getProp("mvnId");
    String strSchema;
    if (id  == null || id.contains("SNAPSHOT")) {
      strSchema = schema.toString();
    } else {
      StringWriter sw = new StringWriter();
      JsonGenerator jgen = Json.FACTORY.createJsonGenerator(sw);
      schema.toJson(new AvroNamesRefResolver(client), jgen);
      jgen.flush();
      strSchema = sw.toString();
    }
    httpHeaders.add(Headers.CONTENT_SCHEMA, strSchema);
    try {
      DatumWriter writer = new ExtendedReflectDatumWriter(schema);
      Encoder encoder = getEncoder(schema, entityStream);
      writer.write(t, encoder);
      encoder.flush();
    } catch (IOException | RuntimeException e) {
      throw new RuntimeException("Serialization failed for " + t, e);
    }
  }

}