package org.spf4j.demo.resources.aql;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.demo.aql.Planet;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.base.CloseableIterable;
import org.spf4j.jaxrs.ProjectionSupport;
import org.spf4j.jaxrs.SqlFilterSupport;
import org.spf4j.security.SecurityContext;

/**
 *
 * @author Zoltan Farkas
 */
@Path("avql/planets")
public class PlanetsResourceImpl implements AvroDataSetContract<Planet> {

  @Override
  public String getName() {
    return "planets";
  }

  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  @ProjectionSupport
  @SqlFilterSupport
  public Iterable<Planet> getData() {
    return Arrays.asList(new Planet("earth", "M", 512731872312L),
            new Planet("vulcan", "M", 612731872312L),
            new Planet("andoria", "M", 602731872312L));
  }

  @Override
  public CloseableIterable<? extends IndexedRecord> getData(final SqlPredicate<Planet> filter,
          final List<String> select, final SecurityContext ctx,
          final long timeout, final TimeUnit timeUnit) {
    return CloseableIterable.from(Iterables.filter(getData(), filter::test));
  }

}
