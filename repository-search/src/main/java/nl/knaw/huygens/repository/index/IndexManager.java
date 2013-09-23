package nl.knaw.huygens.repository.index;

import java.util.Map;

import nl.knaw.huygens.repository.config.Configuration;
import nl.knaw.huygens.repository.config.DocTypeRegistry;
import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.model.Relation;
import nl.knaw.huygens.repository.storage.StorageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This manager is responsible for handling document changes on the index.
 *
 * The manager uses the Solr cores defined in the configuration file.
 * Each core corresponds to a primitive document type and stores data
 * for all subclasses of that document type.
 * Relations are basic infrastructure and need not be specified.
 *
 * Since we are using the 'commitWithin' feature of Solr, there's no need
 * for flushing indexes, except when closing down.
 *
 * The client that instantiates this manager id responsible for calling
 * the close method in order to release the resources used.
 */
@Singleton
public class IndexManager {

  private final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

  private final DocTypeRegistry registry;
  private final LocalSolrServer server;
  private Map<Class<? extends Document>, DocumentIndexer<? extends Document>> indexers;

  @Inject
  public IndexManager(Configuration config, DocTypeRegistry registry, LocalSolrServer server, StorageManager storageManager) {
    this.registry = registry;
    this.server = server;
    setupIndexers(config, storageManager);
  }

  private void setupIndexers(Configuration config, StorageManager storageManager) {
    boolean error = false;
    indexers = Maps.newHashMap();
    indexers.put(Relation.class, new RelationIndexer(registry, storageManager, server));
    for (String doctype : config.getSettings("indexeddoctypes")) {
      Class<? extends Document> type = registry.getTypeForIName(doctype);
      if (type == null) {
        LOG.error("Configuration: '{}' is not a document type", doctype);
        error = true;
      } else if (type != registry.getBaseClass(type)) {
        LOG.error("Configuration: '{}' is not a primitive document type", doctype);
        error = true;
      } else if (indexers.containsKey(type)) {
        LOG.warn("Configuration: ignoring entry '{}' in indexeddoctypes", doctype);
      } else {
        String core = type.getSimpleName().toLowerCase();
        indexers.put(type, DomainDocumentIndexer.newInstance(storageManager, server, core));
      }
    }
    if (error) {
      throw new RuntimeException("Configuration error");
    }
  }

  private <T extends Document> DocumentIndexer<T> indexerForType(Class<T> type) {
    @SuppressWarnings("unchecked")
    DocumentIndexer<T> indexer = (DocumentIndexer<T>) indexers.get(type);
    return (indexer != null) ? indexer : new NoDocumentIndexer<T>();
  }

  public <T extends Document> void addDocument(Class<T> type, String id) throws IndexException {
    addBaseDocument(registry.getBaseClass(type), id);
  }

  private <T extends Document> void addBaseDocument(Class<T> type, String id) throws IndexException {
    indexerForType(type).add(type, id);
  }

  public <T extends Document> void updateDocument(Class<T> type, String id) throws IndexException {
    updateBaseDocument(registry.getBaseClass(type), id);
  }

  private <T extends Document> void updateBaseDocument(Class<T> type, String id) throws IndexException {
    indexerForType(type).modify(type, id);
  }

  public <T extends Document> void deleteDocument(Class<T> type, String id) throws IndexException {
    indexerForType(registry.getBaseClass(type)).remove(id);
  }

  public void deleteAllDocuments() throws IndexException {
    try {
      server.deleteAll();
    } catch (Exception e) {
      throw new IndexException("Failed to delete all documents from index", e);
    }
  }

  public void close() throws IndexException {
    try {
      server.commitAll();
      server.shutdown();
    } catch (Exception e) {
      throw new IndexException("Failed to release IndexManager resources", e);
    }
  }

}
