package org.spf4j.demo.resources.aql;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.demo.aql.Character;
import org.spf4j.aql.AvroDataSetContract;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.avro.schema.Schemas;
import org.spf4j.base.CloseableIterable;
import org.spf4j.jaxrs.CsvParam;
import org.spf4j.jaxrs.IterableArrayContent;
import org.spf4j.security.SecurityContext;

/**
 *
 * @author Zoltan Farkas
 */
@Path("avql/characters")
@Singleton
public class CharactersResourceImpl implements AvroDataSetContract<Character> {

  @Override
  public String getName() {
    return "characters";
  }

  @Override
  public Set<Feature> getFeatures() {
    return ImmutableSet.of(Feature.FILTERABLE, Feature.PROJECTABLE);
  }

  @GET
  @Produces({"application/json", "application/avro+json", "application/avro"})
  @Operation(
         description = "Get Characters",
         responses = {
           @ApiResponse(
                 description = "Charracters",
                 responseCode = "200",
                 content = @Content(array =  @io.swagger.v3.oas.annotations.media.ArraySchema(
                         schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Character.class))))
         }
  )
  public Iterable<? extends IndexedRecord> getData(@QueryParam("_where")
          @Parameter(name = "_where", in = ParameterIn.QUERY,
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class),
            description = "sql where expression", example = "name like 'a%'")
          @Nullable SqlPredicate<Character> filter,
          @QueryParam("_project") @CsvParam @Nullable List<String> project) {
    Iterable<Character> filtered = Iterables.filter(Arrays.asList(new Character("sth1", "James Kirk", "earth", "human"),
            new Character("sth2", "Fips", "earth", "dog"),
            new Character("sth3", "Cica Mama", "earth", "cat"),
            new Character("sth4", "Spock", "vulcan", "vulcan"),
            new Character("sth5", "Thy'lek Shran", "andorian", "andoria")),
            filter == null ? (x) -> true : filter::test);
    if (project != null) {
      Schema sourceSchema = Character.getClassSchema();
      Schema resultSchema = Schemas.project(sourceSchema, project);
      return IterableArrayContent.from(Iterables.transform(filtered,
                                             (x) -> Schemas.project(resultSchema, sourceSchema, x)),
                                       resultSchema);
    } else {
      return IterableArrayContent.from(filtered, Character.getClassSchema());
    }
  }

  @Override
  public long getRowCountStatistic() {
    return 5;
  }

  @Override
  public CloseableIterable<? extends IndexedRecord> getData(final SqlPredicate<Character> filter,
          final List<String> selectProjections, final SecurityContext ctx,
          final long timeout, final TimeUnit timeUnit) {
    return CloseableIterable.from(getData(filter, selectProjections));
  }

}
