
package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.spf4j.aql.DataSetResource;
import org.spf4j.demo.aql.Character;

/**
 *
 * @author Zoltan Farkas
 */
@Path("avql/characters")
public class CharactersResourrceImpl implements DataSetResource<Character> {

  @Override
  public String getName() {
    return "characters";
  }

  @GET
  public Iterable<Character> getData() {
    return Arrays.asList(new Character("sth1", "James Kirk", "earth", "human"),
         new Character("sth2", "Fips", "earth", "dog"),
         new Character("sth3", "Cica Mama", "earth", "cat"),
         new Character("sth4", "Spock", "vulcan", "vulcan"),
         new Character("sth5", "Thy'lek Shran", "andorian", "andoria"));
  }


  @Override
  public Iterable<Character> getData(final String where, final String select) {
    return getData();
  }
}
