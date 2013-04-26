package nl.knaw.huygens.repository.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.knaw.huygens.repository.managers.model.MultipleReferringDoc;
import nl.knaw.huygens.repository.managers.model.ReferredDoc;
import nl.knaw.huygens.repository.managers.model.ReferringDoc;
import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.model.util.DocumentTypeRegister;
import nl.knaw.huygens.repository.persistence.PersistenceException;
import nl.knaw.huygens.repository.persistence.PersistenceManager;
import nl.knaw.huygens.repository.pubsub.Hub;
import nl.knaw.huygens.repository.storage.StorageIterator;
import nl.knaw.huygens.repository.storage.VariationStorage;
import nl.knaw.huygens.repository.variation.model.GeneralTestDoc;
import nl.knaw.huygens.repository.variation.model.TestConcreteDoc;
import nl.knaw.huygens.repository.variation.model.projecta.OtherDoc;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class StorageManagerTest {
  private StorageManager instance;
  private VariationStorage variationStorage;
  private Set<String> documentTypes;
  private Hub hub;
  private DocumentTypeRegister docTypeRegistry;
  private PersistenceManager persistenceManager;

  @Before
  public void SetUp() {
    variationStorage = mock(VariationStorage.class);
    documentTypes = new HashSet<String>();
    hub = mock(Hub.class);
    docTypeRegistry = mock(DocumentTypeRegister.class);
    persistenceManager = mock(PersistenceManager.class);
    instance = new StorageManager(variationStorage, documentTypes, hub, docTypeRegistry, persistenceManager);
  }

  @Test
  public void testGetCompleteDocumentDocumentFound() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    GeneralTestDoc doc = mock(GeneralTestDoc.class);
    when(doc.getId()).thenReturn(id);
    when(doc.getDescription()).thenReturn("test");

    when(variationStorage.getItem(type, id)).thenReturn(doc);

    Document actualDoc = instance.getCompleteDocument(type, id);

    assertEquals(id, actualDoc.getId());
    assertEquals("test", actualDoc.getDescription());
  }

  @Test
  public void testGetCompleteDocumentDocumentNotFound() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    when(variationStorage.getItem(type, id)).thenReturn(null);

    Document actualDoc = instance.getCompleteDocument(type, id);

    assertNull(actualDoc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetCompleteDocumentIOException() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    when(variationStorage.getItem(type, id)).thenThrow(IOException.class);

    Document actualDoc = instance.getCompleteDocument(type, id);

    assertNull(actualDoc);
  }

  @Test
  public void testGetDocumentDocumentFound() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    GeneralTestDoc doc = mock(GeneralTestDoc.class);
    when(doc.getId()).thenReturn(id);
    when(doc.getDescription()).thenReturn("test");

    when(variationStorage.getItem(type, id)).thenReturn(doc);

    Document actualDoc = instance.getDocument(type, id);

    assertEquals(id, actualDoc.getId());
    assertEquals("test", actualDoc.getDescription());
  }

  @Test
  public void testGetDocumentDocumentNotFound() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    when(variationStorage.getItem(type, id)).thenReturn(null);

    Document actualDoc = instance.getDocument(type, id);

    assertNull(actualDoc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetDocumentIOException() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    when(variationStorage.getItem(type, id)).thenThrow(IOException.class);

    Document actualDoc = instance.getDocument(type, id);

    assertNull(actualDoc);
  }

  @Test
  public void testGetCompleteVariationDocumentFound() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";
    String variation = "projecta";

    GeneralTestDoc doc = mock(GeneralTestDoc.class);
    when(doc.getId()).thenReturn(id);
    when(doc.getDescription()).thenReturn("test");

    when(variationStorage.getVariation(type, id, variation)).thenReturn(doc);

    Document actualDoc = instance.getCompleteVariation(type, id, variation);

    assertEquals(id, actualDoc.getId());
    assertEquals("test", actualDoc.getDescription());
  }

  @Test()
  public void testGetCompleteVariationDocumentNotFound() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";
    String variation = "projecta";

    when(variationStorage.getVariation(type, id, variation)).thenReturn(null);

    Document actualDoc = instance.getCompleteVariation(type, id, variation);

    assertNull(actualDoc);
  }

  @SuppressWarnings("unchecked")
  @Test()
  public void testGetCompleteVariationIOException() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";
    String variation = "projecta";

    when(variationStorage.getVariation(type, id, variation)).thenThrow(IOException.class);

    Document actualDoc = instance.getCompleteVariation(type, id, variation);

    assertNull(actualDoc);
  }

  @Test
  public void testGetAllVariationsSuccess() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    GeneralTestDoc doc = mock(GeneralTestDoc.class);
    when(doc.getId()).thenReturn(id);
    when(doc.getDescription()).thenReturn("test");

    GeneralTestDoc doc2 = mock(GeneralTestDoc.class);
    when(doc.getId()).thenReturn(id);
    when(doc.getDescription()).thenReturn("test2");

    when(variationStorage.getAllVariations(type, id)).thenReturn(Lists.newArrayList(doc, doc2));

    List<GeneralTestDoc> actualDocs = instance.getAllVariations(type, id);

    assertEquals(2, actualDocs.size());
  }

  @Test
  public void testGetAllVariationsDocumentNull() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    when(variationStorage.getAllVariations(type, id)).thenReturn(null);

    List<GeneralTestDoc> actualDoc = instance.getAllVariations(type, id);

    assertNull(actualDoc);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetAllVariationsIOException() throws IOException {
    Class<GeneralTestDoc> type = GeneralTestDoc.class;
    String id = "testId";

    when(variationStorage.getAllVariations(type, id)).thenThrow(IOException.class);

    List<GeneralTestDoc> actualDoc = instance.getAllVariations(type, id);

    assertNull(actualDoc);
  }

  @Test
  public void testAddDocumentDocumentAdded() throws IOException {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";

    final List<TestConcreteDoc> storedDocuments = new ArrayList<TestConcreteDoc>();

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {

        TestConcreteDoc addedDoc = (TestConcreteDoc) invocation.getArguments()[1];

        storedDocuments.add(addedDoc);

        return null;
      }
    }).when(variationStorage).addItem(type, doc);

    instance.addDocument(type, doc);

    TestConcreteDoc actualDoc = storedDocuments.get(0);

    assertEquals(doc.name, actualDoc.name);

  }

  @Test(expected = IOException.class)
  public void testAddDocumentStorageException() throws IOException {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doThrow(IOException.class).when(variationStorage).addItem(type, doc);

    instance.addDocument(type, doc);
  }

  @Test
  public void testAddDocumentPersistentException() throws IOException, PersistenceException {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";

    final List<TestConcreteDoc> storedDocuments = new ArrayList<TestConcreteDoc>();

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {

        TestConcreteDoc addedDoc = (TestConcreteDoc) invocation.getArguments()[1];

        storedDocuments.add(addedDoc);

        return null;
      }
    }).when(variationStorage).addItem(type, doc);

    doThrow(PersistenceException.class).when(persistenceManager).persistObject(anyString(), anyString());

    instance.addDocument(type, doc);

    TestConcreteDoc actualDoc = storedDocuments.get(0);

    assertEquals(doc.name, actualDoc.name);
  }

  @Test(expected = IOException.class)
  public void testAddDocumentPublishException() throws Exception {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doThrow(Exception.class).when(hub).publish(any(Object.class));

    instance.addDocument(type, doc);
  }

  @Test
  public void testModifyDocumentDocumentModified() throws IOException {
    TestConcreteDoc expectedDoc = new TestConcreteDoc();
    expectedDoc.name = "test";
    expectedDoc.setId("TCD0000000001");

    final TestConcreteDoc actualDoc = new TestConcreteDoc();
    actualDoc.name = "dtu";
    expectedDoc.setId("TCD0000000001");

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {

        TestConcreteDoc updatedDoc = (TestConcreteDoc) invocation.getArguments()[2];

        actualDoc.name = updatedDoc.name;

        return null;
      }
    }).when(variationStorage).updateItem(type, expectedDoc.getId(), expectedDoc);

    instance.modifyDocument(type, expectedDoc);

    assertEquals(expectedDoc.name, actualDoc.name);

  }

  @Test(expected = IOException.class)
  public void testModifyDocumentStorageException() throws IOException {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";
    doc.setId("TCD0000000001");

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doThrow(IOException.class).when(variationStorage).updateItem(type, doc.getId(), doc);

    instance.modifyDocument(type, doc);
  }

  @Test
  public void testModifyDocumentPersistentException() throws IOException, PersistenceException {
    TestConcreteDoc expectedDoc = new TestConcreteDoc();
    expectedDoc.name = "test";
    expectedDoc.setId("TCD0000000001");

    final TestConcreteDoc actualDoc = new TestConcreteDoc();
    actualDoc.name = "dtu";
    expectedDoc.setId("TCD0000000001");

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {

        TestConcreteDoc updatedDoc = (TestConcreteDoc) invocation.getArguments()[2];

        actualDoc.name = updatedDoc.name;

        return null;
      }
    }).when(variationStorage).updateItem(type, expectedDoc.getId(), expectedDoc);

    instance.modifyDocument(type, expectedDoc);

    assertEquals(expectedDoc.name, actualDoc.name);

  }

  @Test(expected = IOException.class)
  public void testModifyDocumentPublishException() throws Exception {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";
    doc.setId("TCD0000000001");

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doThrow(Exception.class).when(hub).publish(any(Object.class));

    instance.modifyDocument(type, doc);
  }

  @Test
  public void testRemoveDocumentDocumentRemoved() throws IOException {
    TestConcreteDoc inputDoc = new TestConcreteDoc();
    inputDoc.name = "test";
    inputDoc.setId("TCD0000000001");
    inputDoc.setDeleted(true);

    final TestConcreteDoc actualDocument = new TestConcreteDoc();
    actualDocument.name = "test";
    actualDocument.setId("TCD0000000001");
    actualDocument.setDeleted(false);

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {

        actualDocument.setDeleted(true);

        return null;
      }
    }).when(variationStorage).deleteItem(type, inputDoc.getId(), inputDoc.getLastChange());

    instance.removeDocument(type, inputDoc);

    assertTrue(actualDocument.isDeleted());

  }

  @Test(expected = IOException.class)
  public void testRemoveDocumentStorageException() throws IOException {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";
    doc.setId("TCD0000000001");

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doThrow(IOException.class).when(variationStorage).deleteItem(type, doc.getId(), doc.getLastChange());

    instance.removeDocument(type, doc);
  }

  @Test(expected = IOException.class)
  public void testRemoveDocumentPublishException() throws Exception {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.name = "test";
    doc.setId("TCD0000000001");

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    doThrow(Exception.class).when(hub).publish(any(Object.class));

    instance.removeDocument(type, doc);
  }

  @Test
  public void testGetLastChanged() throws IOException {
    List<Document> lastChangeList = Lists.newArrayList(mock(Document.class), mock(Document.class), mock(Document.class));

    when(variationStorage.getLastChanged(anyInt())).thenReturn(lastChangeList);

    List<Document> actualList = instance.getLastChanged(3);

    assertEquals(3, actualList.size());

  }

  @SuppressWarnings("unchecked")
  @Test()
  public void testGetLastChangedIOException() throws IOException {

    when(variationStorage.getLastChanged(anyInt())).thenThrow(IOException.class);

    List<Document> actualList = instance.getLastChanged(3);

    assertTrue(actualList.isEmpty());
  }

  @Test
  public void testGetAllLimited() {
    List<TestConcreteDoc> limitedList = Lists.newArrayList(mock(TestConcreteDoc.class), mock(TestConcreteDoc.class), mock(TestConcreteDoc.class));

    @SuppressWarnings("unchecked")
    StorageIterator<TestConcreteDoc> iterator = mock(StorageIterator.class);
    when(iterator.getSome(anyInt())).thenReturn(limitedList);

    Class<TestConcreteDoc> type = TestConcreteDoc.class;

    when(variationStorage.getAllByType(type)).thenReturn(iterator);

    List<TestConcreteDoc> actualList = instance.getAllLimited(type, 0, 3);

    assertEquals(3, actualList.size());
  }

  @Test
  public void testGetAllLimitedLimitIsZero() {
    List<TestConcreteDoc> documentList = instance.getAllLimited(TestConcreteDoc.class, 0, 0);

    assertTrue(documentList.isEmpty());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetReferringDocs() {
    String referredDocId = "referreddoc";
    String referringDocId = "referringdoc";
    documentTypes.addAll(Sets.newHashSet(referringDocId, referredDocId));

    Class<ReferringDoc> referringDocType = ReferringDoc.class;
    Class<ReferredDoc> referredDocType = ReferredDoc.class;

    doReturn(referredDocType).when(docTypeRegistry).getClassFromTypeString(referredDocId);
    doReturn(referringDocType).when(docTypeRegistry).getClassFromTypeString(referringDocId);

    when(variationStorage.getIdsForQuery(any(Class.class), any(List.class), any(String[].class))).thenReturn(Lists.newArrayList("RFD000000001"));

    instance = new StorageManager(variationStorage, documentTypes, hub, docTypeRegistry, persistenceManager);

    Map<List<String>, List<String>> referringDocs = instance.getReferringDocs(referringDocType, referredDocType, "RDD000000001");

    assertFalse(referringDocs.isEmpty());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetReferringDocsWithMultipleReferringDoc() {
    String referredDocId = "referreddoc";
    String referringDocId = "referringdoc";
    String multipleReferringDocId = "multiplereferringdoc";

    documentTypes.addAll(Sets.newHashSet(referringDocId, referredDocId, multipleReferringDocId));

    Class<ReferringDoc> referringDocType = ReferringDoc.class;
    Class<ReferredDoc> referredDocType = ReferredDoc.class;
    Class<MultipleReferringDoc> multipleReferringDocType = MultipleReferringDoc.class;

    doReturn(referredDocType).when(docTypeRegistry).getClassFromTypeString(referredDocId);
    doReturn(referringDocType).when(docTypeRegistry).getClassFromTypeString(referringDocId);
    doReturn(multipleReferringDocType).when(docTypeRegistry).getClassFromTypeString(multipleReferringDocId);

    when(variationStorage.getIdsForQuery(any(Class.class), any(List.class), any(String[].class))).thenReturn(Lists.newArrayList("RFD000000001", "RDD000000001"));

    instance = new StorageManager(variationStorage, documentTypes, hub, docTypeRegistry, persistenceManager);

    Map<List<String>, List<String>> referringDocs = instance.getReferringDocs(multipleReferringDocType, referredDocType, "RDD000000001");

    assertFalse(referringDocs.isEmpty());
  }

  @Test
  public void testGetReferringDocsNoMappingsForReferringType() {
    String referredDocId = "referreddoc";
    String referringDocId = "referringdoc";
    documentTypes.addAll(Sets.newHashSet(referredDocId, referringDocId));

    Class<ReferringDoc> referringDocType = ReferringDoc.class;
    Class<ReferredDoc> referredDocType = ReferredDoc.class;
    Class<OtherDoc> otherDocType = OtherDoc.class;

    doReturn(referredDocType).when(docTypeRegistry).getClassFromTypeString(referredDocId);
    doReturn(referringDocType).when(docTypeRegistry).getClassFromTypeString(referringDocId);

    instance = new StorageManager(variationStorage, documentTypes, hub, docTypeRegistry, persistenceManager);

    Map<List<String>, List<String>> referringDocs = instance.getReferringDocs(otherDocType, referredDocType, "RDD000000001");

    assertTrue(referringDocs.isEmpty());
  }

  @Test
  public void testGetReferringDocsNoMappingsForReferredType() {
    String referredDocId = "referreddoc";
    String referringDocId = "referringdoc";
    documentTypes.addAll(Sets.newHashSet(referredDocId, referringDocId));

    Class<ReferringDoc> referringDocType = ReferringDoc.class;
    Class<ReferredDoc> referredDocType = ReferredDoc.class;
    Class<OtherDoc> otherDocType = OtherDoc.class;

    doReturn(referredDocType).when(docTypeRegistry).getClassFromTypeString(referredDocId);
    doReturn(referringDocType).when(docTypeRegistry).getClassFromTypeString(referringDocId);

    instance = new StorageManager(variationStorage, documentTypes, hub, docTypeRegistry, persistenceManager);

    Map<List<String>, List<String>> referringDocs = instance.getReferringDocs(referringDocType, otherDocType, "RDD000000001");

    assertTrue(referringDocs.isEmpty());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetReferringDocsReferringDocsNotFound() {
    String referredDocId = "referreddoc";
    String referringDocId = "referringdoc";
    documentTypes.addAll(Sets.newHashSet(referringDocId, referredDocId));

    Class<ReferringDoc> referringDocType = ReferringDoc.class;
    Class<ReferredDoc> referredDocType = ReferredDoc.class;

    doReturn(referredDocType).when(docTypeRegistry).getClassFromTypeString(referredDocId);
    doReturn(referringDocType).when(docTypeRegistry).getClassFromTypeString(referringDocId);

    when(variationStorage.getIdsForQuery(any(Class.class), any(List.class), any(String[].class))).thenReturn(Lists.<ReferringDoc> newArrayList());

    instance = new StorageManager(variationStorage, documentTypes, hub, docTypeRegistry, persistenceManager);

    Map<List<String>, List<String>> referringDocs = instance.getReferringDocs(referringDocType, referredDocType, "RDD000000001");

    assertTrue(referringDocs.isEmpty());
  }

}
