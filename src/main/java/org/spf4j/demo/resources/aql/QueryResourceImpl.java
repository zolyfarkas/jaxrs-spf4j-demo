package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.Frameworks;
import org.spf4j.avro.calcite.AvroProjectableFilterableTable;
import org.spf4j.base.CloseableIterator;
import org.spf4j.demo.aql.AvroQueryResource;
import org.spf4j.demo.aql.DataSetResource;
import org.spf4j.jaxrs.StreamingArrayContent;

/**
 *
 * @author Zoltan Farkas
 */
public class QueryResourceImpl implements AvroQueryResource {

  public QueryResourceImpl(final Iterable<DataSetResource> resources) {
    SchemaPlus schema = Frameworks.createRootSchema(true);
    for (DataSetResource res : resources) {
      schema.add(res.getName(), new AvroProjectableFilterableTable(res.getSchema(),
              () -> CloseableIterator.from(res.getData(null, null).iterator())));
    }
  }


  @Override
  public StreamingArrayContent<GenericRecord> query(String query) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Map<String, Schema> schema() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Schema entitySchema(String enttityName) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
