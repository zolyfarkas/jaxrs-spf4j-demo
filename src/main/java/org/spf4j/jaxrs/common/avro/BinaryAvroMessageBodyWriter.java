package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

/**
 * @author Zoltan Farkas
 */
@Provider
@Produces({"application/octet-stream;fmt=avro", "application/avro"})
public class BinaryAvroMessageBodyWriter extends  AvroMessageBodyWriter {

  @Inject
  public BinaryAvroMessageBodyWriter(final SchemaResolver client) {
    super(client);
  }

  @Override
  public Encoder getEncoder(Schema writerSchema, OutputStream os) throws IOException {
    return EncoderFactory.get().binaryEncoder(os, null);
  }


}
