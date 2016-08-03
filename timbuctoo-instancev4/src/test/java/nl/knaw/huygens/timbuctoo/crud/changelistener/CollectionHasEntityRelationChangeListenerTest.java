package nl.knaw.huygens.timbuctoo.crud.changelistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import nl.knaw.huygens.timbuctoo.model.vre.Collection;
import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import nl.knaw.huygens.timbuctoo.util.TestGraphBuilder;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static nl.knaw.huygens.timbuctoo.util.TestGraphBuilder.newGraph;
import static nl.knaw.huygens.timbuctoo.util.VertexMatcher.likeVertex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class CollectionHasEntityRelationChangeListenerTest {

  @Test
  public void onCreateCreatesAHasEntityRelationWithTheCollectionsFromTheTypesProperty() throws JsonProcessingException {
    final String[] types = {"person", "wwperson"};
    final String typesJson = new ObjectMapper().writeValueAsString(types);
    final String linkingNodeWwPersonsName = "linking-node-wwpersons";
    final String linkingNodePersonsName = "linking-node-persons";
    final String createThisVertexName = "create-this-vertex";
    final TestGraphBuilder builder = newGraph()
      .withVertex(linkingNodePersonsName, v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", linkingNodePersonsName)
      )
      .withVertex(linkingNodeWwPersonsName, v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", linkingNodeWwPersonsName)
      )
      .withVertex("other", v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", "other")
      )
      .withVertex(v -> v
        .withLabel(Collection.DATABASE_LABEL)
        .withProperty(Collection.ENTITY_TYPE_NAME_PROPERTY_NAME, "person")
        .withProperty(Collection.COLLECTION_NAME_PROPERTY_NAME, "persons")
        .withOutgoingRelation(Collection.HAS_ENTITY_NODE_RELATION_NAME, linkingNodePersonsName)
      )
      .withVertex(v -> v
        .withLabel(Collection.DATABASE_LABEL)
        .withProperty(Collection.ENTITY_TYPE_NAME_PROPERTY_NAME, "wwperson")
        .withProperty(Collection.COLLECTION_NAME_PROPERTY_NAME, "wwpersons")
        .withOutgoingRelation(Collection.HAS_ENTITY_NODE_RELATION_NAME, linkingNodeWwPersonsName)
      )
      .withVertex(v -> v
        .withProperty("types", typesJson)
        .withTimId(createThisVertexName)
      );
    final Graph graph = builder.build();
    final GraphWrapper graphWrapper = builder.wrap();
    Vertex createdVertex = graph.traversal().V().has("tim_id", createThisVertexName).next();

    new CollectionHasEntityRelationChangeListener(graphWrapper).onCreate(createdVertex);

    final List<Vertex> actualLinkingNodes = Lists.newArrayList(
      createdVertex.vertices(Direction.IN, Collection.HAS_ENTITY_RELATION_NAME));

    assertThat(actualLinkingNodes, containsInAnyOrder(
      likeVertex().withProperty("check", linkingNodePersonsName),
      likeVertex().withProperty("check", linkingNodeWwPersonsName)
    ));
  }

  @Test
  public void onUpdateRemovesHasEntityRelationsToCollectionsRemovedFromTheTypesProperty()
    throws JsonProcessingException {
    final String[] types = {"person"};
    final String typesJson = new ObjectMapper().writeValueAsString(types);
    final String linkingNodeWwPersonsName = "linking-node-wwpersons";
    final String linkingNodePersonsName = "linking-node-persons";
    final String updateThisVertexName = "update-this-vertex";

    final TestGraphBuilder builder = newGraph()
      .withVertex(linkingNodePersonsName, v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", linkingNodePersonsName)
      )
      .withVertex(linkingNodeWwPersonsName, v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", linkingNodeWwPersonsName)
      )
      .withVertex("other", v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", "other")
      )
      .withVertex(v -> v
        .withLabel(Collection.DATABASE_LABEL)
        .withProperty(Collection.ENTITY_TYPE_NAME_PROPERTY_NAME, "person")
        .withProperty(Collection.COLLECTION_NAME_PROPERTY_NAME, "persons")
        .withOutgoingRelation(Collection.HAS_ENTITY_NODE_RELATION_NAME, linkingNodePersonsName)
      )
      .withVertex(v -> v
        .withLabel(Collection.DATABASE_LABEL)
        .withProperty(Collection.ENTITY_TYPE_NAME_PROPERTY_NAME, "wwperson")
        .withProperty(Collection.COLLECTION_NAME_PROPERTY_NAME, "wwpersons")
        .withOutgoingRelation(Collection.HAS_ENTITY_NODE_RELATION_NAME, linkingNodeWwPersonsName)
      )
      .withVertex(v -> v
        .withProperty("types", typesJson)
        .withTimId(updateThisVertexName)
        .withIncomingRelation(Collection.HAS_ENTITY_RELATION_NAME, linkingNodePersonsName)
        .withIncomingRelation(Collection.HAS_ENTITY_RELATION_NAME, linkingNodeWwPersonsName)
      );
    final Graph graph = builder.build();
    final GraphWrapper graphWrapper = builder.wrap();

    Vertex updatedVertex = graph.traversal().V().has("tim_id", updateThisVertexName).next();

    new CollectionHasEntityRelationChangeListener(graphWrapper)
      .onUpdate(Optional.empty(), updatedVertex);

    final List<Vertex> actualLinkingNodes = Lists.newArrayList(
      updatedVertex.vertices(Direction.IN, Collection.HAS_ENTITY_RELATION_NAME));

    assertThat(actualLinkingNodes, containsInAnyOrder(
      likeVertex().withProperty("check", linkingNodePersonsName)
    ));
  }

  @Test
  public void onUpdateAddsHasEntityRelationsToCollectionAddedToTheTypesProperty()
    throws JsonProcessingException {
    final String[] types = {"person", "wwperson"};
    final String typesJson = new ObjectMapper().writeValueAsString(types);
    final String linkingNodeWwPersonsName = "linking-node-wwpersons";
    final String linkingNodePersonsName = "linking-node-persons";
    final String updateThisVertexName = "update-this-vertex";

    final TestGraphBuilder builder = newGraph()
      .withVertex(linkingNodePersonsName, v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", linkingNodePersonsName)
      )
      .withVertex(linkingNodeWwPersonsName, v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", linkingNodeWwPersonsName)
      )
      .withVertex("other", v -> v
        .withLabel(Collection.COLLECTION_ENTITIES_LABEL)
        .withProperty("check", "other")
      )
      .withVertex(v -> v
        .withLabel(Collection.DATABASE_LABEL)
        .withProperty(Collection.ENTITY_TYPE_NAME_PROPERTY_NAME, "person")
        .withProperty(Collection.COLLECTION_NAME_PROPERTY_NAME, "persons")
        .withOutgoingRelation(Collection.HAS_ENTITY_NODE_RELATION_NAME, linkingNodePersonsName)
      )
      .withVertex(v -> v
        .withLabel(Collection.DATABASE_LABEL)
        .withProperty(Collection.ENTITY_TYPE_NAME_PROPERTY_NAME, "wwperson")
        .withProperty(Collection.COLLECTION_NAME_PROPERTY_NAME, "wwpersons")
        .withOutgoingRelation(Collection.HAS_ENTITY_NODE_RELATION_NAME, linkingNodeWwPersonsName)

      )
      .withVertex(v -> v
        .withProperty("types", typesJson)
        .withTimId(updateThisVertexName)
        .withIncomingRelation(Collection.HAS_ENTITY_RELATION_NAME, linkingNodePersonsName)
      );
    final Graph graph = builder.build();
    final GraphWrapper graphWrapper = builder.wrap();

    Vertex updatedVertex = graph.traversal().V().has("tim_id", updateThisVertexName).next();

    new CollectionHasEntityRelationChangeListener(graphWrapper)
      .onUpdate(Optional.empty(), updatedVertex);

    final List<Vertex> actualLinkingNodes = Lists.newArrayList(
      updatedVertex.vertices(Direction.IN, Collection.HAS_ENTITY_RELATION_NAME));

    assertThat(actualLinkingNodes, containsInAnyOrder(
      likeVertex().withProperty("check", linkingNodePersonsName),
      likeVertex().withProperty("check", linkingNodeWwPersonsName)
    ));
  }
}