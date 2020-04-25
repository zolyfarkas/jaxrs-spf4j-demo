
package org.spf4j.demo;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 *
 * @author Zoltan Farkas
 */
public class TestFeature  implements Feature {

  private final String namespace;

  @Inject
  public TestFeature(@ConfigProperty(name = "spf4j.failsafe.retryLogLevel") String namespace) {
    if (!"DEBUG".equals(namespace)) {
      throw new RuntimeException();
    }
    this.namespace = namespace;
  }


  @Override
  public boolean configure(FeatureContext arg0) {
    return true;
  }

}
