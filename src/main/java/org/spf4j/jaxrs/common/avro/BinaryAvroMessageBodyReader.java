package org.spf4j.jaxrs.common.avro;

import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.ext.Provider;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;

/**
 * @author Zoltan Farkas
 */
@Provider
@Consumes({"application/octet-stream;fmt=avro", "application/avro"})
public class BinaryAvroMessageBodyReader extends AvroMessageBodyReader {


  @Inject
  public BinaryAvroMessageBodyReader(final SchemaResolver client) {
    super(client);
  }

  @Override
  public Decoder getDecoder(Schema writerSchema, InputStream is) throws IOException {
    return DecoderFactory.get().binaryDecoder(is, null);
  }

}
