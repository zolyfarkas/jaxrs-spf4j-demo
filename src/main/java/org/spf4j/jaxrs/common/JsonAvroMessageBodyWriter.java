package org.spf4j.jaxrs.common;

import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ExtendedJsonEncoder;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/json;fmt=avro", "text/plain;fmt=avro"})
public class JsonAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  @Inject
  public JsonAvroMessageBodyWriter(final SchemaResolver client) {
    super(client);
  }

  @Override
  public Encoder getEncoder(Schema writerSchema, OutputStream os) throws IOException {
    return new ExtendedJsonEncoder(writerSchema, os);
  }


}
