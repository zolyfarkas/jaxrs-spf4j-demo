package org.spf4j.demo.resources.aql;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.security.PermitAll;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.demo.aql.Friendship;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.base.CloseableIterable;
import org.spf4j.security.AbacSecurityContext;

/**
 * @author Zoltan Farkas
 */
@Singleton
@Path("avql/friendships")
@PermitAll
public class FriendshipResourceImpl implements AvroDataSetContract<Friendship> {

  @Override
  public String getName() {
    return "friendships";
  }

  @Override
  public Set<Feature> getFeatures() {
    return ImmutableSet.of(Feature.FILTERABLE);
  }

  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  public Iterable<Friendship> getData(@QueryParam("_where") @Nullable SqlPredicate<Friendship> filter) {
    return Iterables.filter(Arrays.asList(new Friendship("sth1", "sth2"),
            new Friendship("sth1", "sth3")), filter == null ? (x) -> true : filter::test);
  }

  @Override
  public long getRowCountStatistic() {
    return 2;
  }

  public CloseableIterable<? extends IndexedRecord> getData(@Nullable SqlPredicate<Friendship> filter,
          List<String> select, final AbacSecurityContext ctx,
          final long timeout, final TimeUnit timeUnit) {
    return CloseableIterable.from(getData(filter));
  }

}

