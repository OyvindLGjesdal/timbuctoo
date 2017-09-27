package nl.knaw.huygens.timbuctoo.v5.berkeleydb;

import com.sleepycat.bind.tuple.TupleBinding;
import nl.knaw.huygens.timbuctoo.v5.dropwizard.BdbNonPersistentEnvironmentCreator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

import static com.sleepycat.bind.tuple.TupleBinding.getPrimitiveBinding;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BdbWrapperTest {

  private BdbNonPersistentEnvironmentCreator creator;
  private BdbWrapper<String, String> database;

  @Before
  public void setUp() throws Exception {
    creator = new BdbNonPersistentEnvironmentCreator();
    creator.start();

    final TupleBinding<String> binder = getPrimitiveBinding(String.class);
    database = creator.getDatabase("a", "b", "test", true, binder, binder);

    database.put("aa", "bb");
    database.put("ab", "ac");
    database.put("ab", "bb");
    database.put("ab", "bc");
    database.put("ab", "dd");
    database.put("bb", "bb");
  }

  @After
  public void close() {
    creator.stop();
  }


  @Test
  public void getAllItems() throws Exception {
    final Stream<String> stream = database.databaseGetter()
      .getAll()
      .getValues();
    final long count = stream
      .count();
    stream.close();
    assertThat(count, is(6L));
  }

  @Test
  public void getAllItemsWithSameKey() throws Exception {
    final Stream<String> stream = database.databaseGetter()
      .key("ab")
      .dontSkip()
      .forwards()
      .getValues();
    final long count = stream
      .count();
    stream.close();
    assertThat(count, is(4L));
  }

  @Test
  public void getAllItemsWithSamePrefix() throws Exception {
    final Stream<String> stream = database.databaseGetter()
      .partialKey("a", (prefix, key) -> key.startsWith(prefix))
      .dontSkip()
      .forwards()
      .getValues();
    final long count = stream
      .count();
    stream.close();
    assertThat(count, is(5L));
  }

  @Test
  public void getItemBackwards() throws Exception {
    final Stream<String> stream = database.databaseGetter()
      .key("ab")
      .skipToEnd()
      .backwards()
      .getValues();
    final long count = stream
      .count();
    stream.close();
    assertThat(count, is(4L));
  }

  @Test
  public void getItemFromValue() throws Exception {
    final Stream<String> stream = database.databaseGetter()
      .key("ab")
      .skipToValue("bc")
      .forwards()
      .getValues();
    final long count = stream
      .count();
    stream.close();
    assertThat(count, is(2L));
  }

  @Test
  public void getItemFromValueRange() throws Exception {
    final Stream<String> stream = database.databaseGetter()
      .key("ab")
      .skipNearValue("b")
      .onlyValuesMatching((prefix, value) -> {
        return value.startsWith(prefix);
      })
      .forwards()
      .getValues();
    final long count = stream
      .count();
    stream.close();
    assertThat(count, is(2L));
  }

}