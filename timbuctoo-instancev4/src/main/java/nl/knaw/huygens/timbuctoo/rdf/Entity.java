package nl.knaw.huygens.timbuctoo.rdf;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.List;

public class Entity {
  private final Vertex vertex;
  private final List<CollectionDescription> collectionDescriptions;

  public Entity(Vertex vertex, List<CollectionDescription> collectionDescriptions) {
    this.vertex = vertex;
    this.collectionDescriptions = collectionDescriptions;
  }

  public void addProperty(String propertyName, String value) {
    collectionDescriptions.forEach(collectionDescription -> vertex.property(
      collectionDescription.createPropertyName(propertyName), value));
  }

  public void addToCollection(Collection collection) {
    collectionDescriptions.add(collection.getDescription());
    collection.add(vertex, collectionDescriptions);
  }

  public Relation addRelation(RelationType relationType, Entity other) {
    Edge edge = vertex.addEdge(relationType.getRegularName(), other.vertex);

    return new Relation(edge, relationType);
  }
}
