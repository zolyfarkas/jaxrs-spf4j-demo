package org.spf4j.actuator.health;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Zoltan Farkas
 */
@Service
public final class ClusterAllNodesRegistration implements HealthCheck.Registration {

  private final ClusterAllNodesCheck check;

  @Inject
  public ClusterAllNodesRegistration(final ClusterAllNodesCheck check) {
    this.check = check;
  }

  @Override
  public String[] getPath() {
    return new String [] {"allNodesHealth"};
  }

  @Override
  public HealthCheck getCheck() {
    return check;
  }

}
