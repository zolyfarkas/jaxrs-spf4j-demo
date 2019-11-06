package org.spf4j.demo.resources.aql;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.demo.aql.Friendship;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.base.CloseableIterable;
import org.spf4j.security.SecurityContext;

/**
 * @author Zoltan Farkas
 */
@Path("avql/friendships")
public class FriendshipResourceImpl implements AvroDataSetContract<Friendship> {

  @Override
  public String getName() {
    return "friendships";
  }


  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  public Iterable<Friendship> getData(@QueryParam("_where") @Nullable Predicate<Friendship> filter) {
    return Iterables.filter(Arrays.asList(new Friendship("sth1", "sth2"),
            new Friendship("sth1", "sth3")), filter == null ? (x) -> true : filter::test);
  }

  public CloseableIterable<? extends IndexedRecord> getData(@Nullable Predicate<Friendship> filter,
          List<String> select, final SecurityContext ctx,
          final long timeout, final TimeUnit timeUnit) {
    return CloseableIterable.from(getData(filter));
  }

}
