
package org.spf4j.jaxrs.common.avro;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.codehaus.jackson.JsonParser;
import org.spf4j.avro.SchemaClient;

/**
 * registers all avro stuff.
 * @author Zoltan Farkas
 */
public class AvroFeature implements Feature {

  static {
    org.apache.avro.Schema.FACTORY.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
  }

  private final SchemaClient client;

  @Inject
  public AvroFeature(final SchemaClient client) {
    this.client = client;
  }



  @Override
  public boolean configure(FeatureContext context) {
    context.register(new JsonAvroMessageBodyReader(client));
    context.register(new BinaryAvroMessageBodyReader(client));
    context.register(new JsonAvroMessageBodyWriter(client));
    context.register(new BinaryAvroMessageBodyWriter(client));
    context.register(new SchemaMessageBodyReader());
    context.register(new SchemaMessageBodyWriter());
    context.register(new AvroParameterConverterProvider(client));
    return true;
  }

}
