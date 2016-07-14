package nl.knaw.huygens.timbuctoo.rdf;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import static nl.knaw.huygens.timbuctoo.rdf.Database.RDF_URI_PROP;

public class RelationType {

  private Vertex relationTypeVertex;

  public RelationType(Vertex relationTypeVertex) {
    this.relationTypeVertex = relationTypeVertex;
  }

  public String getRegularName() {
    return relationTypeVertex.value("relationtype_regularName");
  }

  public String getRdfUri() {
    return relationTypeVertex.value(RDF_URI_PROP);
  }
}
