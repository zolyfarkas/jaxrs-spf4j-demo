package org.spf4j.demo.resources.aql;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.demo.aql.Species;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.base.CloseableIterable;
import org.spf4j.security.SecurityContext;


@Path("avql/species")
public class SpeciesResourceImpl  implements AvroDataSetContract<Species> {

  @Override
  public String getName() {
    return "species";
  }

  @Override
  public Set<Feature> getFeatures() {
    return ImmutableSet.of(Feature.FILTERABLE);
  }

  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  public Iterable<Species> getData(@QueryParam("_where") @Nullable SqlPredicate<Species> filter) {
    return Iterables.filter(Arrays.asList(new Species("cat", 15, "earth"),
        new Species("dog", 13, "earth"),
        new Species("human", 70, "earth"),
        new Species("andorian", 70, "andoria"),
        new Species("vulcan", 70, "vulcan")), filter == null ? (x) -> true : filter::test);
  }

  @Override
  public CloseableIterable<? extends IndexedRecord> getData(@Nullable SqlPredicate<Species> filter,
          List<String> select, final SecurityContext ctx,
          final long timeout, final TimeUnit timeUnit) {
    return CloseableIterable.from(getData(filter));
  }

}
