package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import javax.ws.rs.Path;
import org.spf4j.demo.aql.DataSetResource;
import org.spf4j.demo.aql.Species;


@Path("avql/species")
public class SpeciesResourceImpl  implements DataSetResource<Species> {

  @Override
  public String getName() {
    return "species";
  }

  @Override
  public Iterable<Species> getData(String where, String select) {
    return Arrays.asList(new Species("cat", 15, "earth"),
        new Species("dog", 13, "earth"),
        new Species("human", 70, "earth"),
        new Species("andorian", 70, "andoria"),
        new Species("vulcan", 70, "vulcan"));
  }

}
