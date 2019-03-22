
package org.spf4j.actuator.health;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Zoltan Farkas
 */
public class HealthOrgNodeTest {

  private static final Logger LOG = LoggerFactory.getLogger(HealthOrgNodeTest.class);

  @Test
  public void testHealthOrgNode() {
    HealthOrgNode checks = HealthOrgNode.newHealthChecks();
    checks.addHealthCheck(HealthCheck.NOP, "local", "time");
    checks.addHealthCheck(HealthCheck.NOP, "local", "jdbc");
    checks.addHealthCheck(HealthCheck.NOP, "cluster", "versions");
    LOG.debug("checks:{}", checks);
    Assert.assertSame(HealthCheck.NOP, checks.getHealthCheck("local", "time"));
    checks.traverse((path, check) -> LOG.debug("{}/{}", path, check), "local");
  }

}
