package org.spf4j.avro;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.base.avro.PackageInfo;

/**
 * @author Zoltan Farkas
 */
public class ExtendedReflectDataTest {

  private static final Logger LOG = LoggerFactory.getLogger(SchemaClientTest.class);

  public <T> List<T> testMethod(Class<T> clasz) {
    return null;
  }

  public <T> List<GenericRecord> testMethod2(Class<T> clasz) {
    return null;
  }

  @Test
  public void testParameterizedTypes() throws NoSuchMethodException {
    ExtendedReflectData rdata = new ExtendedReflectData();
    Method m = ExtendedReflectDataTest.class.getMethod("testMethod", new Class[] {Class.class});
    Type rt = m.getGenericReturnType();
    Schema createSchema = rdata.getSchema(rt, "T", String.class);
    LOG.debug("schema", createSchema);
  }

  @Test
  public void testParameterizedTypes2() throws NoSuchMethodException {
    ExtendedReflectData rdata = new ExtendedReflectData();
    Method m = ExtendedReflectDataTest.class.getMethod("testMethod2", new Class[] {Class.class});
    Type rt = m.getGenericReturnType();
    Schema createSchema = rdata.createSchema(rt, Arrays.asList(new PackageInfo("a", "b")), new HashMap<>());
    LOG.debug("schema", createSchema);
  }

}
