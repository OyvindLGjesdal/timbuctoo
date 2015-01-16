package nl.knaw.huygens.timbuctoo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.util.Calendar;
import java.util.Date;

import nl.knaw.huygens.timbuctoo.config.TypeRegistry;
import nl.knaw.huygens.timbuctoo.model.SearchResult;
import nl.knaw.huygens.timbuctoo.model.SystemEntity;
import nl.knaw.huygens.timbuctoo.storage.RelationTypes;
import nl.knaw.huygens.timbuctoo.storage.Storage;
import nl.knaw.huygens.timbuctoo.storage.StorageException;
import nl.knaw.huygens.timbuctoo.storage.StorageIterator;
import nl.knaw.huygens.timbuctoo.storage.ValidationException;
import nl.knaw.huygens.timbuctoo.storage.mongo.MongoDBIntegrationTestHelper;
import nl.knaw.huygens.timbuctoo.util.RelationRefCreatorFactory;
import nl.knaw.huygens.timbuctoo.vre.VRECollection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RepositoryIntegrationTest {
  private static final Class<SearchResult> SEARCH_RESULT_TYPE = SearchResult.class;
  private Repository instance;
  private RelationRefCreatorFactory relationRefCreatorFactoryMock;
  private MongoDBIntegrationTestHelper dbIntegrationTestHelper;

  @Before
  public void setup() throws Exception {
    dbIntegrationTestHelper = new MongoDBIntegrationTestHelper();
    dbIntegrationTestHelper.startCleanDB();

    TypeRegistry registry = TypeRegistry.getInstance();
    Storage storage = dbIntegrationTestHelper.createStorage(registry);
    relationRefCreatorFactoryMock = mock(RelationRefCreatorFactory.class);

    instance = new Repository(registry, storage, mock(VRECollection.class), relationRefCreatorFactoryMock, new RelationTypes(storage));
  }

  @After
  public void tearDown() {
    dbIntegrationTestHelper.stopDB();
  }

  @Test
  public void deleteSearchResultByDateRemovesAllSearchResultsFromTheDatabaseThatAreCreatedBeforeACertainDate() throws Exception {
    // setup
    // 2 searches made today
    addSearchResult();
    addSearchResult();

    Date tomorrow = getDateDaysFromToday(1);

    assertThatDatabaseContainsNumberOfItemsOfType(SEARCH_RESULT_TYPE, 2);

    // action
    instance.deleteSearchResultsBefore(tomorrow);

    // verify
    assertThatDatabaseContainsNumberOfItemsOfType(SEARCH_RESULT_TYPE, 0);
  }

  @Test
  public void deleteSearchResultByDateDoesNotRemoveSearchAfterTheSpecifiedDate() throws Exception {
    // setup
    // 2 searches made today
    addSearchResult();
    addSearchResult();

    Date yesterday = getDateDaysFromToday(-1);

    assertThatDatabaseContainsNumberOfItemsOfType(SEARCH_RESULT_TYPE, 2);

    // action
    instance.deleteSearchResultsBefore(yesterday);

    // verify
    assertThatDatabaseContainsNumberOfItemsOfType(SEARCH_RESULT_TYPE, 2);
  }

  private <T extends SystemEntity> void assertThatDatabaseContainsNumberOfItemsOfType(Class<T> type, int size) {
    StorageIterator<T> foundResults = instance.getSystemEntities(type);

    assertThat(foundResults, is(notNullValue()));
    assertThat(foundResults.size(), is(equalTo(size)));
  }

  private Date getDateDaysFromToday(int daysFromToday) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, daysFromToday);
    Date date = cal.getTime();
    return date;
  }

  private void addSearchResult() throws StorageException, ValidationException {
    SearchResult searchResult = new SearchResult();

    instance.addSystemEntity(SEARCH_RESULT_TYPE, searchResult);
  }
}
