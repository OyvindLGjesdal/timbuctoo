package nl.knaw.huygens.timbuctoo.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nl.knaw.huygens.timbuctoo.crud.InvalidCollectionException;
import nl.knaw.huygens.timbuctoo.crud.UrlGenerator;
import nl.knaw.huygens.timbuctoo.database.TimbuctooActions;
import nl.knaw.huygens.timbuctoo.database.dto.QuickSearch;
import nl.knaw.huygens.timbuctoo.database.dto.ReadEntity;
import nl.knaw.huygens.timbuctoo.database.dto.RelationRef;
import nl.knaw.huygens.timbuctoo.database.dto.dataset.Collection;
import nl.knaw.huygens.timbuctoo.database.dto.property.TimProperty;
import nl.knaw.huygens.timbuctoo.model.Change;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsn;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnA;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.hamcrest.MockitoHamcrest.intThat;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class AutocompleteServiceTest {

  @Test(expected = InvalidCollectionException.class)
  public void searchThrowsWhenTheCollectionNameDoesNotExist() throws InvalidCollectionException {
    TimbuctooActions timbuctooActions = mock(TimbuctooActions.class);
    given(timbuctooActions.getCollectionMetadata(anyString())).willThrow(new InvalidCollectionException(""));
    AutocompleteService underTest = new AutocompleteService(null, timbuctooActions);
    underTest.search("nonexistent", Optional.empty(), Optional.empty());
  }

  @Test
  public void searchConvertsTheReadEntityToJson() throws Exception {
    UUID id = UUID.randomUUID();
    String collectionName = "wwpersons";
    ReadEntity entity = ReadEntityStubs.readEntityWithDisplayNameIdAndRev("[TEMP] An author", id, 2);
    TimbuctooActions timbuctooActions = mock(TimbuctooActions.class);
    given(timbuctooActions.getCollectionMetadata(anyString()))
      .willReturn(CollectionStubs.collWithCollectionName(collectionName));
    given(timbuctooActions.doQuickSearch(any(), any(), anyInt())).willReturn(Lists.newArrayList(entity));
    AutocompleteService instance = new AutocompleteService(
      (collection, id1, rev) -> URI.create("http://example.com/" + collection + "/" + id1 + "?rev=" + rev),
      timbuctooActions
    );

    String query = "*author*";
    JsonNode result = instance.search(collectionName, Optional.of(query), Optional.empty());

    assertThat(result.toString(), sameJSONAs(jsnA(
      jsnO("value", jsn("[TEMP] An author"), "key", jsn("http://example.com/wwpersons/" + id.toString() + "?rev=2"))
    ).toString()));
    verify(timbuctooActions).doQuickSearch(
      argThat(hasProperty("collectionName", equalTo(collectionName))),
      any(QuickSearch.class),
      intThat(is(50))
    );
  }

  @Test
  public void searchFiltersKeywordsByType() throws InvalidCollectionException {
    String query = "*foo bar*";
    String keywordType = "maritalStatus";
    String collectionName = "wwkeywords";
    UUID id = UUID.randomUUID();
    ReadEntity readEntity = ReadEntityStubs.readEntityWithDisplayNameIdAndRev("a keyword", id, 2);
    TimbuctooActions timbuctooActions = mock(TimbuctooActions.class);
    given(timbuctooActions.getCollectionMetadata(anyString()))
      .willReturn(CollectionStubs.keywordCollWithCollectionName(collectionName));
    given(timbuctooActions.doKeywordQuickSearch(any(), anyString(), any(), anyInt()))
      .willReturn(Lists.newArrayList(readEntity));
    UrlGenerator urlGenerator =
      (coll, id1, rev) -> URI.create("http://example.com/" + coll + "/" + id1 + "?rev=" + rev);
    AutocompleteService instance = new AutocompleteService(
      urlGenerator,
      timbuctooActions);

    JsonNode result = instance.search(collectionName, Optional.of(query), Optional.of(keywordType));

    assertThat(result.toString(), sameJSONAs(jsnA(
      jsnO("value", jsn("a keyword"), "key", jsn("http://example.com/wwkeywords/" + id.toString() + "?rev=2"))
    ).toString()));
    verify(timbuctooActions).doKeywordQuickSearch(
      argThat(hasProperty("collectionName", equalTo(collectionName))),
      argThat(is(keywordType)),
      any(QuickSearch.class),
      intThat(is(50))
    );
  }

  @Test
  public void searchRequests1000ResultsWhenTheQueryIsEmpty() throws Exception {
    UUID id = UUID.randomUUID();
    String collectionName = "wwpersons";
    ReadEntity entity = ReadEntityStubs.readEntityWithDisplayNameIdAndRev("[TEMP] An author", id, 2);
    TimbuctooActions timbuctooActions = mock(TimbuctooActions.class);
    given(timbuctooActions.getCollectionMetadata(anyString()))
      .willReturn(CollectionStubs.collWithCollectionName(collectionName));
    given(timbuctooActions.doQuickSearch(any(), any(), anyInt())).willReturn(Lists.newArrayList(entity));
    AutocompleteService instance = new AutocompleteService(
      (collection, id1, rev) -> URI.create("http://example.com/" + collection + "/" + id1 + "?rev=" + rev),
      timbuctooActions
    );

    instance.search(collectionName, Optional.empty(), Optional.empty());

    verify(timbuctooActions).doQuickSearch(
      argThat(hasProperty("collectionName", equalTo(collectionName))),
      any(QuickSearch.class),
      intThat(is(1000))
    );
  }

  // TODO move to database.dto test package
  private static class ReadEntityStubs {
    public static ReadEntity readEntityWithDisplayNameIdAndRev(String displayName, UUID id, int rev) {
      return new ReadEntity() {
        @Override
        public Iterable<TimProperty<?>> getProperties() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public int getRev() {
          return rev;
        }

        @Override
        public boolean getDeleted() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public String getPid() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public URI getRdfUri() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<String> getTypes() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Change getModified() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public Change getCreated() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<RelationRef> getRelations() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public String getDisplayName() {
          return displayName;
        }

        @Override
        public UUID getId() {
          return id;
        }

        @Override
        public Map<String, Object> getExtraProperties() {
          throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public List<String> getRdfAlternatives() {
          throw new UnsupportedOperationException("Not implemented yet");
        }
      };
    }
  }

  private static class CollectionStubs {
    public static Collection collWithCollectionName(String collectionName) {
      // TODO find a better way to create a collection for a test
      return new Collection(null, collectionName, null, Maps.newLinkedHashMap(), collectionName, null, null, false,
        false);
    }

    public static Collection keywordCollWithCollectionName(String collectionName) {
      return new Collection(null, "keyword", null, Maps.newLinkedHashMap(), collectionName, null, null, false, false);
    }
  }
}
