package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.demo.aql.Character;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.base.CloseableIterable;

/**
 *
 * @author Zoltan Farkas
 */
@Path("avql/characters")
public class CharactersResourceImpl implements AvroDataSetContract<Character> {

  @Override
  public String getName() {
    return "characters";
  }

  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  public Iterable<Character> getData() {
    return Arrays.asList(new Character("sth1", "James Kirk", "earth", "human"),
            new Character("sth2", "Fips", "earth", "dog"),
            new Character("sth3", "Cica Mama", "earth", "cat"),
            new Character("sth4", "Spock", "vulcan", "vulcan"),
            new Character("sth5", "Thy'lek Shran", "andorian", "andoria"));
  }

  @Override
  public CloseableIterable<? extends IndexedRecord> getData(final Predicate<Character> filter,
          final List<String> selectProjections, final long timeout, final TimeUnit timeUnit) {
    return CloseableIterable.from(getData());
  }

}
