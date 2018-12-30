package org.spf4j.jaxrs.common;

import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.ExtendedJsonDecoder;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/json;fmt=avro", "text/plain;fmt=avro"})
public class JsonAvroMessageBodyReader extends AvroMessageBodyReader {


  @Inject
  public JsonAvroMessageBodyReader(final SchemaResolver client) {
    super(client);
  }

  @Override
  public Decoder getDecoder(Schema writerSchema, InputStream is) throws IOException {
    return new ExtendedJsonDecoder(writerSchema, is);
  }


}
