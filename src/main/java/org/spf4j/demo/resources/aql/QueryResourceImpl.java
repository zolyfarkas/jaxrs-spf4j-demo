package org.spf4j.demo.resources.aql;

import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Path;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.calcite.config.Lex;
import org.apache.calcite.interpreter.Interpreter;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.avro.calcite.AvroProjectableFilterableTable;
import org.spf4j.avro.calcite.EmbededDataContext;
import org.spf4j.avro.calcite.IndexedRecords;
import org.spf4j.avro.calcite.PlannerUtils;
import org.spf4j.avro.calcite.Types;
import org.spf4j.base.CloseableIterator;
import org.spf4j.base.Writeable;
import org.spf4j.demo.aql.AvroQueryResource;
import org.spf4j.demo.aql.DataSetResource;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.log.ExecContextLogger;

/**
 * @author Zoltan Farkas
 */
@Path("avql/query")
public class QueryResourceImpl implements AvroQueryResource {

  private static final Logger LOG = new ExecContextLogger(LoggerFactory.getLogger(QueryResourceImpl.class));

  private final Planner planner;

  @Inject
  public QueryResourceImpl(final Iterable<DataSetResource> resources) {
    SchemaPlus schema = Frameworks.createRootSchema(true);
    for (DataSetResource res : resources) {
      String name = res.getName();
      LOG.debug("Registered {} table to schema", name);
      schema.add(name, new AvroProjectableFilterableTable(res.getSchema(),
              () -> CloseableIterator.from(res.getData(null, null).iterator())));
    }

    SqlParser.Config cfg = SqlParser.configBuilder()
            .setCaseSensitive(true)
            .setIdentifierMaxLength(255)
            .setLex(Lex.JAVA).build();
    FrameworkConfig config = Frameworks.newConfigBuilder()
            .parserConfig(cfg)
            .defaultSchema(schema).build();
    this.planner = Frameworks.getPlanner(config);
  }


  @Override
  public IterableArrayContent<GenericRecord> query(final String query) {
    SqlNode parse;
    try {
      parse = planner.parse(query);
    } catch (SqlParseException ex) {
      throw new ClientErrorException("Cannot parse query: " + query, 400, ex);
    }
    try {
      parse = planner.validate(parse);
    } catch (ValidationException ex) {
      throw new ClientErrorException("Cannot validate query: " + query, 400, ex);
    }
    RelRoot rel;
    try {
      rel = planner.rel(parse);
    } catch (RelConversionException ex) {
      throw new RuntimeException(ex);
    }
    RelNode relNode = PlannerUtils.pushDownPredicatesAndProjection(rel.project());
    LOG.debug("exec plan: {}", new Object() {
      @Override
      public String toString() {
        return RelOptUtil.toString(relNode);
      }
    });
    RelDataType rowType = relNode.getRowType();
    LOG.debug("Return row type: {}", rowType);
    Schema from = Types.from(rowType);
    LOG.debug("Return row schema: {}", from);
    Interpreter interpreter = new Interpreter(new EmbededDataContext(new JavaTypeFactoryImpl()), relNode);
    return new IterableArrayContent<GenericRecord>() {
      @Override
      public Schema getElementSchema() {
        return from;
      }

      @Override
      public void close() {
        interpreter.close();
      }

      @Override
      public Iterator<GenericRecord> iterator() {
        return new Iterator<GenericRecord>() {
          private final Iterator<Object[]>  it = interpreter.iterator();

          @Override
          public boolean hasNext() {
            return it.hasNext();
          }

          @Override
          public GenericRecord next() {
            Object[] row = it.next();
            LOG.debug("RawRow {}",  row);
            GenericRecord record = IndexedRecords.fromRecord(from, row);
            LOG.debug("Row",  record);
            return record;
          }
        };
      }
    };
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
