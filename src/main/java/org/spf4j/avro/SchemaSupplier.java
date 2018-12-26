package org.spf4j.avro;

import org.apache.avro.Schema;

/**
 * @author Zoltan Farkas
 */
public interface SchemaSupplier {

  Schema get(String id);

}
