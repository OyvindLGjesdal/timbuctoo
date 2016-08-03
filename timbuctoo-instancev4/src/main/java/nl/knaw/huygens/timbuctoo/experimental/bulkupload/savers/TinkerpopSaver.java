package nl.knaw.huygens.timbuctoo.experimental.bulkupload.savers;

import nl.knaw.huygens.timbuctoo.experimental.bulkupload.parsingstatemachine.ImportPropertyDescription;
import nl.knaw.huygens.timbuctoo.experimental.bulkupload.parsingstatemachine.ImportPropertyDescriptions;
import nl.knaw.huygens.timbuctoo.model.vre.Vre;
import nl.knaw.huygens.timbuctoo.model.vre.Vres;
import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Map;
import java.util.UUID;

public class TinkerpopSaver implements AutoCloseable, Saver {
  public static final String RAW_COLLECTION_EDGE_NAME = "hasRawCollection";
  public static final String RAW_ITEM_EDGE_NAME = "hasItem";
  public static final String RAW_COLLECTION_NAME_PROPERTY_NAME = "name";
  public static final String FIRST_RAW_ITEM_EDGE_NAME = "hasFirstItem";
  public static final String NEXT_RAW_ITEM_EDGE_NAME = "hasNextItem";
  public static final String RAW_PROPERTY_EDGE_NAME = "hasProperty";
  public static final String FIRST_RAW_PROPERTY_EDGE_NAME = "hasFirstProperty";
  public static final String NEXT_RAW_PROPERTY_EDGE_NAME = "hasNextProperty";
  public static final String RAW_PROPERTY_NAME = "name";
  private final Vres vres;
  private final GraphWrapper graphWrapper;
  private final Vertex vre;
  private final int maxVerticesPerTransaction;
  private final CollectionAdder collectionAdder;
  private int saveCounter;
  private Transaction tx;

  public TinkerpopSaver(Vres vres, GraphWrapper graphWrapper, String vreName, int maxVerticesPerTransaction) {
    this(vres, graphWrapper, vreName, maxVerticesPerTransaction, new CollectionAdder(graphWrapper));
  }

  public TinkerpopSaver(Vres vres, GraphWrapper graphWrapper, String vreName, int maxVerticesPerTransaction,
                        CollectionAdder collectionAdder) {
    this.vres = vres;
    this.graphWrapper = graphWrapper;
    tx = graphWrapper.getGraph().tx();
    this.maxVerticesPerTransaction = maxVerticesPerTransaction;
    this.vre = initVre(vreName);
    this.collectionAdder = collectionAdder;
  }

  private Vertex initVre(String vreName) {
    //FIXME namespace vrename per user

    Vertex result = null;
    try (Transaction tx = graphWrapper.getGraph().tx()) {
      final GraphTraversal<Vertex, Vertex> vre = graphWrapper.getGraph().traversal().V()
                                                             .hasLabel(Vre.DATABASE_LABEL)
                                                             .has(Vre.VRE_NAME_PROPERTY_NAME, vreName);
      if (vre.hasNext()) {
        result = vre.next();
        result.vertices(Direction.BOTH, RAW_COLLECTION_EDGE_NAME).forEachRemaining(coll -> {
          coll.vertices(Direction.BOTH, RAW_ITEM_EDGE_NAME).forEachRemaining(vertex -> {
            vertex.remove();
          });
          coll.remove();
        });
      } else {
        result = graphWrapper.getGraph().addVertex(T.label, Vre.DATABASE_LABEL, Vre.VRE_NAME_PROPERTY_NAME, vreName);
      }
      tx.commit();
    }

    vres.reload();
    return result;
  }

  private void allowCommit() {
    if (saveCounter++ > maxVerticesPerTransaction) {
      saveCounter = 0;
      tx.commit();
      tx = graphWrapper.getGraph().tx();
    }
  }

  @Override
  public void close() {
    tx.commit();
  }

  @Override
  public Vertex addEntity(Vertex rawCollection, Map<String, Object> currentProperties) {
    allowCommit();

    Vertex result = graphWrapper.getGraph().addVertex("tim_id", UUID.randomUUID().toString());
    currentProperties.forEach(result::property);

    collectionAdder.addToLastItemOfCollection(rawCollection, result);
    collectionAdder.addToCollection(rawCollection, result);

    return result;
  }

  @Override
  public Vertex addCollection(String collectionName) {
    Vertex collection = graphWrapper.getGraph().addVertex(RAW_COLLECTION_NAME_PROPERTY_NAME, collectionName);
    vre.addEdge(RAW_COLLECTION_EDGE_NAME, collection);
    return collection;
  }

  @Override
  public void addPropertyDescriptions(Vertex collection, ImportPropertyDescriptions importPropertyDescriptions) {
    Graph graph = graphWrapper.getGraph();
    Vertex previousPropertyDesc = null;
    for (ImportPropertyDescription importPropertyDescription : importPropertyDescriptions) {
      Vertex propertyDesc = graph.addVertex();
      propertyDesc.property("id", importPropertyDescription.getId());
      propertyDesc.property(RAW_PROPERTY_NAME, importPropertyDescription.getPropertyName());
      propertyDesc.property("order", importPropertyDescription.getOrder());

      collection.addEdge(RAW_PROPERTY_EDGE_NAME, propertyDesc);

      if (previousPropertyDesc == null) {
        collection.addEdge(FIRST_RAW_PROPERTY_EDGE_NAME, propertyDesc);
      } else {
        previousPropertyDesc.addEdge(NEXT_RAW_PROPERTY_EDGE_NAME, propertyDesc);
      }

      previousPropertyDesc = propertyDesc;
    }

  }

}