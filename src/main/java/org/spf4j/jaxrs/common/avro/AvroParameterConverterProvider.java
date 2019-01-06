package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.codehaus.jackson.JsonGenerator;
import org.spf4j.base.Json;

/**
 * @author Zoltan Farkas
 */
@Provider
public class AvroParameterConverterProvider implements ParamConverterProvider {


  private final  ParamConverter<Schema> schemaConv;

  public AvroParameterConverterProvider(SchemaResolver client) {
    this.schemaConv =new ParamConverter<Schema>() {
        @Override
        public Schema fromString(String value) {
          return new Schema.Parser(new AvroNamesRefResolver(client)).parse(value);
        }

        @Override
        public String toString(Schema schema) {
          try {
            StringWriter sw = new StringWriter();
            JsonGenerator jgen = Json.FACTORY.createJsonGenerator(sw);
            schema.toJson(new AvroNamesRefResolver(client), jgen);
            jgen.flush();
            return sw.toString();
          } catch (IOException ex) {
            throw new UncheckedIOException(ex);
          }
        }
      };
  }

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (rawType == Schema.class) {
      return (ParamConverter<T>) schemaConv;
    }
    return null;
  }

}
