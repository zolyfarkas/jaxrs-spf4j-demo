
package org.spf4j.fred;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.SchemaClient;
import org.spf4j.http.DefaultDeadlineProtocol;
import org.spf4j.jaxrs.client.Spf4JClient;
import org.spf4j.jaxrs.client.Spf4jWebTarget;
import org.spf4j.jaxrs.client.providers.ClientCustomExecutorServiceProvider;
import org.spf4j.jaxrs.client.providers.ClientCustomScheduledExecutionServiceProvider;
import org.spf4j.jaxrs.client.providers.ExecutionContextClientFilter;
import org.spf4j.jaxrs.common.avro.AvroFeature;

/**
 *
 * @author Zoltan Farkas
 */
public class TestFred {

  private static final Logger LOG = LoggerFactory.getLogger(TestFred.class);

 @Test
 public void getCategories() throws URISyntaxException {
   DefaultDeadlineProtocol dp = new DefaultDeadlineProtocol();
   SchemaClient schemaClient  = new SchemaClient(new URI("https://dl.bintray.com/zolyfarkas/core"));
   AvroFeature avroFeature = new AvroFeature(schemaClient);
   Spf4JClient client = new Spf4JClient(ClientBuilder
           .newBuilder()
           .connectTimeout(2, TimeUnit.SECONDS)
           .readTimeout(60, TimeUnit.SECONDS)
           .register(new ExecutionContextClientFilter(dp))
           .register(ClientCustomExecutorServiceProvider.class)
           .register(ClientCustomScheduledExecutionServiceProvider.class)
           .register(avroFeature)
           .property(ClientProperties.USE_ENCODING, "gzip")
           .build());

   Spf4jWebTarget target = client.target("https://api.stlouisfed.org/fred");
   CategoryResource ct = WebResourceFactory.newResource(CategoryResource.class, target);
   Categories categories = ct.getCategories(0, System.getProperty("fred.apiKey"), "json");
   LOG.debug("Fred categories", categories);

 }

}
