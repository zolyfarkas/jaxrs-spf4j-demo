/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.demo;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.Method;

/**
 *
 * @author Zoltan Farkas
 */
public class TestMethod {


  private static final Logger LOG = LoggerFactory.getLogger(TestMethod.class);


  @Test
  public void testHashCodeEquals() {
    Method m = new Method("org.apache.avro.Schema", "toString");
    Method m2 = new Method("org.apache.avro.Schema", "toString");
    LOG.debug("{} {} {}", m.hashCode(), m2.hashCode(), m.equals(m2));
  }

}
