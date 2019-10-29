package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import javax.ws.rs.Path;
import org.spf4j.demo.aql.DataSetResource;
import org.spf4j.demo.aql.Friendship;

/**
 * @author Zoltan Farkas
 */
@Path("avql/friendships")
public class FriendshipResourceImpl implements DataSetResource<Friendship> {

  @Override
  public Iterable<Friendship> getData(String where, String select) {
    return Arrays.asList(new Friendship("sth1", "sth2"),
            new Friendship("sth1", "sth3"));
  }
;

}
