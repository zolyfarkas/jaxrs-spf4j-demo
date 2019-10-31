
package org.spf4j.demo.resources.aql;

import java.util.Arrays;
import javax.ws.rs.Path;
import org.spf4j.demo.aql.Character;
import org.spf4j.demo.aql.DataSetResource;

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



  @Override
  public Iterable<Character> getData(String where, String select) {
    return Arrays.asList(new Character("sth1", "James Kirk", "earth", "human"),
         new Character("sth2", "Fips", "earth", "dog"),
         new Character("sth3", "Cica Mama", "earth", "cat"),
         new Character("sth4", "Spock", "vulcan", "vulcan"));
  }
}
