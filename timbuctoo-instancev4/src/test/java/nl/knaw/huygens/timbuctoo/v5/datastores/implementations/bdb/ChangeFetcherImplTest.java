package nl.knaw.huygens.timbuctoo.v5.datastores.implementations.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import nl.knaw.huygens.timbuctoo.v5.datastores.quadstore.dto.CursorQuad;
import nl.knaw.huygens.timbuctoo.v5.dropwizard.BdbNonPersistentEnvironmentCreator;
import org.junit.Test;

import java.util.stream.Stream;

import static nl.knaw.huygens.timbuctoo.v5.datastores.quadstore.dto.Direction.OUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ChangeFetcherImplTest {

  @Test
  public void showsAdditions() throws Exception {
    final BdbNonPersistentEnvironmentCreator databaseCreator = new BdbNonPersistentEnvironmentCreator();
    final BdbTripleStore bdbTripleStore = new BdbTripleStore(databaseCreator.getDatabase(
      "a",
      "b",
      "rdfData",
      true,
      TupleBinding.getPrimitiveBinding(String.class),
      TupleBinding.getPrimitiveBinding(String.class)
    ));
    final BdbTruePatchStore truePatchStore = new BdbTruePatchStore(databaseCreator.getDatabase(
      "a",
      "b",
      "truePatch",
      true,
      TupleBinding.getPrimitiveBinding(String.class),
      TupleBinding.getPrimitiveBinding(String.class)
    ));

    bdbTripleStore.putQuad("subj", "pred", OUT, "obj", null, null);
    truePatchStore.put("subj", 0, "pred", OUT, true, "obj", null, null);

    ChangeFetcherImpl changeFetcher = new ChangeFetcherImpl(truePatchStore, bdbTripleStore, 0);

    try (Stream<CursorQuad> predicates = changeFetcher.getPredicates("subj", "pred", OUT, false, false, true)) {
      assertThat(predicates.count(), is(1L));
    }
    try (Stream<CursorQuad> predicates = changeFetcher.getPredicates("subj", "pred", OUT, true, true, true)) {
      assertThat(predicates.count(), is(1L));
    }
    try (Stream<CursorQuad> predicates = changeFetcher.getPredicates("subj", "pred", OUT, true, false, true)) {
      assertThat(predicates.count(), is(1L));
    }

    bdbTripleStore.putQuad("subj", "pred", OUT, "obj2", null, null);
    truePatchStore.put("subj", 1, "pred", OUT, true, "obj2", null, null);
    changeFetcher = new ChangeFetcherImpl(truePatchStore, bdbTripleStore, 1);

    try (Stream<CursorQuad> predicates = changeFetcher.getPredicates("subj", "pred", OUT, false, false, true)) {
      assertThat(predicates.count(), is(1L));
    }
    try (Stream<CursorQuad> predicates = changeFetcher.getPredicates("subj", "pred", OUT, true, true, true)) {
      assertThat(predicates.count(), is(2L));
    }
    try (Stream<CursorQuad> predicates = changeFetcher.getPredicates("subj", "pred", OUT, true, false, true)) {
      assertThat(predicates.count(), is(1L));
    }
  }

}
