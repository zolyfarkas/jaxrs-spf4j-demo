package org.spf4j.avro;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificData;

/**
 * @author Zoltan Farkas
 */
@Beta
public class Projections {

  private static final Configuration CONF = Configuration.defaultConfiguration().jsonProvider(new AvroPathProvider());

  public static Object project(Schema toSchema, Schema fromSchema, Object object) {
    if (toSchema == fromSchema) {
      return object;
    }
    String path = toSchema.getProp("path");
    if (path != null) {
      return JsonPath.parse(object, CONF).read(path);
    }
    Schema.Type type = toSchema.getType();
    if (fromSchema.getType() != type) {
      throw new IllegalArgumentException("Unable to project " + object + " to " + toSchema);
    }
    switch (type) {
      case INT:
      case LONG:
      case FLOAT:
      case STRING:
      case ENUM:
      case FIXED:
      case BOOLEAN:
      case BYTES:
      case DOUBLE:
      case NULL:
        return object;
      case ARRAY:
        List from = (List) object;
        List to = new ArrayList(from.size());
        for (Object o : from) {
          to.add(project(toSchema.getElementType(), fromSchema.getElementType(), o));
        }
        return to;
      case MAP:
        Map<String, Object> fromMap = (Map<String, Object>) object;
        Map<String, Object> toMap = Maps.newLinkedHashMapWithExpectedSize(fromMap.size());
        for (Map.Entry<String, Object> entry : fromMap.entrySet()) {
          toMap.put(entry.getKey(), project(toSchema.getValueType(), fromSchema.getValueType(), entry.getValue()));
        }
        return toMap;
      case UNION:
        if (object == null) {
          return null;
        }
        // matching here is a but flaky... union with multiple erecord types will not work...
        // to be resolved later, this is a demo :-).
        Schema objReflSchema = ReflectData.get().getSchema(object.getClass());
        for (Schema matching : toSchema.getTypes()) {
          if (matching.getType() == objReflSchema.getType()) {
            return project(matching, objReflSchema, object);
          }
        }
        throw new IllegalArgumentException("Unable to project " + object + " to " + toSchema);
      case RECORD:
        GenericData.Record record = new SpecificData.Record(toSchema);
        GenericRecord fromRec = (GenericRecord) object;
        for (Field field : toSchema.getFields()) {
          String fpath = field.getProp("path");
          if (fpath != null) {
            record.put(field.pos(), JsonPath.parse(object, CONF).read(fpath));
          } else {
            record.put(field.pos(), fromRec.get(field.name()));
          }
        }
        return record;
      default:
        throw new IllegalStateException("Unsupported type " + type);
    }
  }

}
