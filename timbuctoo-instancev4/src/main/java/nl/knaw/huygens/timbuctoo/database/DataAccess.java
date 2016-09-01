package nl.knaw.huygens.timbuctoo.database;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import javaslang.control.Try;
import nl.knaw.huygens.timbuctoo.crud.EdgeManipulator;
import nl.knaw.huygens.timbuctoo.crud.EntityFetcher;
import nl.knaw.huygens.timbuctoo.crud.NotFoundException;
import nl.knaw.huygens.timbuctoo.database.dto.Entity;
import nl.knaw.huygens.timbuctoo.database.dto.EntityRelation;
import nl.knaw.huygens.timbuctoo.database.dto.ImmutableEntityRelation;
import nl.knaw.huygens.timbuctoo.database.dto.RelationRef;
import nl.knaw.huygens.timbuctoo.database.dto.RelationType;
import nl.knaw.huygens.timbuctoo.database.dto.property.TimProperty;
import nl.knaw.huygens.timbuctoo.database.dto.property.TinkerPopPropertyConverter;
import nl.knaw.huygens.timbuctoo.database.dto.property.UnknownPropertyException;
import nl.knaw.huygens.timbuctoo.database.exceptions.ObjectSuddenlyDisappearedException;
import nl.knaw.huygens.timbuctoo.database.exceptions.RelationNotPossibleException;
import nl.knaw.huygens.timbuctoo.model.Change;
import nl.knaw.huygens.timbuctoo.model.properties.LocalProperty;
import nl.knaw.huygens.timbuctoo.model.properties.ReadableProperty;
import nl.knaw.huygens.timbuctoo.model.vre.Collection;
import nl.knaw.huygens.timbuctoo.model.vre.Vre;
import nl.knaw.huygens.timbuctoo.model.vre.Vres;
import nl.knaw.huygens.timbuctoo.security.AuthorizationException;
import nl.knaw.huygens.timbuctoo.security.AuthorizationUnavailableException;
import nl.knaw.huygens.timbuctoo.security.Authorizer;
import nl.knaw.huygens.timbuctoo.server.GraphWrapper;
import org.apache.tinkerpop.gremlin.neo4j.process.traversal.LabelP;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.timbuctoo.database.VertexDuplicator.duplicateVertex;
import static nl.knaw.huygens.timbuctoo.logging.Logmarkers.configurationFailure;
import static nl.knaw.huygens.timbuctoo.logging.Logmarkers.databaseInvariant;
import static nl.knaw.huygens.timbuctoo.model.GraphReadUtils.getEntityTypesOrDefault;
import static nl.knaw.huygens.timbuctoo.model.GraphReadUtils.getProp;
import static nl.knaw.huygens.timbuctoo.model.properties.converters.Converters.arrayToEncodedArray;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsn;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnA;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnO;
import static nl.knaw.huygens.timbuctoo.util.StreamIterator.stream;
import static org.slf4j.LoggerFactory.getLogger;

public class DataAccess {

  private static final Logger LOG = getLogger(DataAccess.class);
  private final GraphWrapper graphwrapper;
  private final EntityFetcher entityFetcher;
  private final Authorizer authorizer;
  private final ChangeListener listener;
  private Vres mappings;

  public DataAccess(GraphWrapper graphwrapper, EntityFetcher entityFetcher, Authorizer authorizer,
                    ChangeListener listener, Vres mappings) {
    this.graphwrapper = graphwrapper;
    this.entityFetcher = entityFetcher;
    this.authorizer = authorizer;
    this.listener = listener;
    this.mappings = mappings;
  }

  public DataAccessMethods start() {
    return new DataAccessMethods(graphwrapper, authorizer, listener);
  }

  public Entity getEntity(UUID id, Integer rev, Collection collection) throws NotFoundException {
    final Map<String, ReadableProperty> mapping = collection.getReadableProperties();
    final String entityTypeName = collection.getEntityTypeName();
    final GraphTraversalSource traversalSource = graphwrapper.getGraph().traversal();

    Vertex entityTs;
    try {
      entityTs = entityFetcher.getEntity(traversalSource, id, rev, collection.getCollectionName()).next();
    } catch (NoSuchElementException e) {
      throw new NotFoundException();
    }
    GraphTraversal<Vertex, Vertex> entityT = traversalSource.V(entityTs.id());

    if (!entityT.asAdmin().clone().hasNext()) {
      throw new NotFoundException();
    }

    String entityTypesStr = getProp(entityT.asAdmin().clone().next(), "types", String.class).orElse("[]");
    if (!entityTypesStr.contains("\"" + collection.getEntityTypeName() + "\"")) {
      throw new NotFoundException();
    }

    final List<TimProperty<?>> properties = Lists.newArrayList();
    TinkerPopPropertyConverter dbPropertyConverter = new TinkerPopPropertyConverter(collection);

    GraphTraversal[] propertyGetters = mapping
      .entrySet().stream()
      //append error handling and resulting to the traversal
      .map(prop -> prop.getValue().traversalRaw().sideEffect(x ->
        x.get()
         .onSuccess(value -> {
           try {
             properties.add(dbPropertyConverter.from(prop.getKey(), value));
           } catch (UnknownPropertyException e) {
             LOG.error("Unknown property", e);
           } catch (IOException e) {
             LOG.error(
               databaseInvariant,
               "Property '" + prop.getKey() + "' is not encoded correctly",
               e.getCause()
             );
           }
         })
         .onFailure(e -> {
           if (e.getCause() instanceof IOException) {
             LOG.error(
               databaseInvariant,
               "Property '" + prop.getKey() + "' is not encoded correctly",
               e.getCause()
             );
           } else {
             LOG.error("Something went wrong while reading the property '" + prop.getKey() + "'.", e.getCause());
           }
         })
      ))
      .toArray(GraphTraversal[]::new);

    entityT.asAdmin().clone().union(propertyGetters).forEachRemaining(x -> {
      //Force side effects to happen
    });

    Entity entity = new Entity();
    entity.setProperties(properties);

    Vertex entityVertex = entityT.asAdmin().clone().next();
    // TODO make use converters for the types
    entity.setRev(getProp(entityVertex, "rev", Integer.class).orElse(-1));
    entity.setDeleted(getProp(entityVertex, "deleted", Boolean.class).orElse(false));
    entity.setPid(getProp(entityVertex, "pid", String.class).orElse(null));


    Optional<String> typesOptional = getProp(entityVertex, "types", String.class);
    if (typesOptional.isPresent()) {
      try {
        List<String> types = new ObjectMapper().readValue(typesOptional.get(), new TypeReference<List<String>>() {
        });
        entity.setTypes(types);
      } catch (Exception e) {
        LOG.error(databaseInvariant, "Error while generating variation refs", e);
        entity.setTypes(Lists.newArrayList(entityTypeName));
      }
    } else {
      entity.setTypes(Lists.newArrayList(entityTypeName));
    }

    Optional<String> modifiedStringOptional = getProp(entityVertex, "modified", String.class);
    if (modifiedStringOptional.isPresent()) {
      try {
        entity.setModified(new ObjectMapper().readValue(modifiedStringOptional.get(), Change.class));
      } catch (IOException e) {
        LOG.error(databaseInvariant, "Change cannot be converted", e);
        entity.setModified(new Change());
      }
    } else {
      entity.setModified(new Change());
    }

    Optional<String> createdStringOptional = getProp(entityVertex, "created", String.class);
    if (createdStringOptional.isPresent()) {
      try {
        entity.setCreated(new ObjectMapper().readValue(createdStringOptional.get(), Change.class));
      } catch (IOException e) {
        LOG.error(databaseInvariant, "Change cannot be converted", e);
        entity.setCreated(new Change());
      }
    } else {
      entity.setCreated(new Change());
    }

    entity.setRelations(getRelations(entityVertex, traversalSource, collection));
    return entity;
  }

  // TODO make private when this method is not used in the TinkerpopJsonCrudService
  public Optional<String> getDisplayname(GraphTraversalSource traversalSource, Vertex vertex,
                                         Collection targetCollection) {
    ReadableProperty displayNameProperty = targetCollection.getDisplayName();
    if (displayNameProperty != null) {
      GraphTraversal<Vertex, Try<JsonNode>> displayNameGetter = traversalSource.V(vertex.id()).union(
        targetCollection.getDisplayName().traversalJson()
      );
      if (displayNameGetter.hasNext()) {
        Try<JsonNode> traversalResult = displayNameGetter.next();
        if (!traversalResult.isSuccess()) {
          LOG.error(databaseInvariant, "Retrieving displayname failed", traversalResult.getCause());
        } else {
          if (traversalResult.get() == null) {
            LOG.error(databaseInvariant, "Displayname was null");
          } else {
            if (!traversalResult.get().isTextual()) {
              LOG.error(databaseInvariant, "Displayname was not a string but " + traversalResult.get().toString());
            } else {
              return Optional.of(traversalResult.get().asText());
            }
          }
        }
      } else {
        LOG.error(databaseInvariant, "Displayname traversal resulted in no results: " + displayNameGetter);
      }
    } else {
      LOG.warn("No displayname configured for " + targetCollection.getEntityTypeName());
    }
    return Optional.empty();
  }

  private List<RelationRef> getRelations(Vertex entity, GraphTraversalSource traversalSource,
                                        Collection collection) {
    final Vre vre = collection.getVre();
    Vre adminVre = mappings.getVre("Admin");
    Map<String, Collection> collectionsOfVre = vre.getCollections();

    Object[] relationTypes = traversalSource.V().has(T.label, LabelP.of("relationtype")).id().toList().toArray();

    GraphTraversal<Vertex, RelationRef> realRelations = collectionsOfVre.values().stream()
      .filter(Collection::isRelationCollection)
      .findAny()
      .map(Collection::getEntityTypeName)
      .map(ownRelationType -> traversalSource
        .V(entity.id())
        .union(
          __.outE()
            .as("edge")
            .label().as("label")
            .select("edge"),
          __.inE()
            .as("edge")
            .label().as("edgeLabel")
            .V(relationTypes)
            .has("relationtype_regularName",
              __.where(P.eq("edgeLabel")))
            .properties("relationtype_inverseName").value()
            .as("label")
            .select("edge")
        )
        .where(
          //FIXME move to strategy
          __.has("isLatest", true)
            .not(__.has("deleted", true))
            .not(__.hasLabel("VERSION_OF"))
            //The old timbuctoo showed relations from all
            // VRE's.
            // Changing that behaviour caused breakage in
            // the
            //frontend and exposed errors in the database
            // that
            //.has("types", new P<>((val, def) -> val
            // .contains
            // ("\"" + ownRelationType + "\""), ""))
            // FIXME: string concatenating methods like this
            // should be delegated to a configuration clas
            .not(
              __.has(ownRelationType + "_accepted", false))
        )
        .otherV().as("vertex")
        .select("edge", "vertex", "label")
        .map(r -> {
          try {
            Map<String, Object> val = r.get();
            Edge edge = (Edge) val.get("edge");
            Vertex target = (Vertex) val.get("vertex");
            String label = (String) val.get("label");

            String targetEntityType =  vre.getOwnType(getEntityTypesOrDefault(target));
            Collection targetCollection = vre.getCollectionForTypeName(targetEntityType);
            if (targetEntityType == null) {
              //this means that the edge is of this VRE, but the
              // Vertex it points to is of another VRE
              //In that case we use the admin vre
              targetEntityType = adminVre.getOwnType(getEntityTypesOrDefault(target));
              targetCollection = adminVre.getCollectionForTypeName(targetEntityType);
            }

            String displayName = getDisplayname(traversalSource, target, targetCollection)
                                           .orElse("<No displayname found>");
            String uuid = getProp(target, "tim_id", String.class).orElse("");
            boolean accepted = getProp(edge, "accepted", Boolean.class).orElse(true);
            String relationId =
              getProp(edge, "tim_id", String.class)
                .orElse("");
            int relationRev = getProp(edge, "rev", Integer.class).orElse(1);

            return new RelationRef(uuid, targetCollection.getCollectionName(), targetEntityType, accepted, relationId,
              relationRev, label, displayName);
          } catch (Exception e) {
            LOG.error(databaseInvariant,
              "Something went wrong while formatting the entity",
              e);
            return null;
          }
        })
      )
      .orElse(EmptyGraph.instance().traversal().V()
                        .map(x -> null));

    List<RelationRef> relations = stream(realRelations)
      .filter(x -> x != null).collect(toList());

    return relations;
  }


  public static class DataAccessMethods implements AutoCloseable {
    private final Transaction transaction;
    private final Authorizer authorizer;
    private final ChangeListener listener;
    private final GraphTraversalSource traversal;
    private Optional<Boolean> isSuccess = Optional.empty();

    private DataAccessMethods(GraphWrapper graphWrapper, Authorizer authorizer, ChangeListener listener) {
      this.transaction = graphWrapper.getGraph().tx();
      this.authorizer = authorizer;
      this.listener = listener;
      if (!transaction.isOpen()) {
        transaction.open();
      }
      this.traversal = graphWrapper.getGraph().traversal();
    }


    public void success() {
      isSuccess = Optional.of(true);
    }

    public void rollback() {
      isSuccess = Optional.of(false);
    }

    @Override
    public void close() {
      if (isSuccess.isPresent()) {
        if (isSuccess.get()) {
          transaction.commit();
        } else {
          transaction.rollback();
        }
        transaction.close();
      } else {
        transaction.rollback();
        transaction.close();
        LOG.error("Transaction was not closed, rolling back. Please add an explicit rollback so that we know this " +
          "was not a missing success()");
      }
    }

    /**
     * Creates a relation between two entities.
     *
     * <p>If a relation already exists, it will not create a new one.</p>
     * @param sourceId Id of the source Entity
     * @param typeId Id of the relation type
     * @param targetId Id of the target Entity
     * @param collection the relation collection (not the collection of the source or target vertices)
     * @param userId the user under which the relation is created. Will be validated and written to the database
     * @param instant the time under which the acceptance should be recorded
     *
     * @return the UUID of the relation
     *
     * @throws RelationNotPossibleException if a relation is not possible
     * @throws AuthorizationUnavailableException if the relation datafile cannot be read
     * @throws AuthorizationException if the user is not authorized
     */
    public UUID acceptRelation(UUID sourceId, UUID typeId, UUID targetId, Collection collection, String userId,
                               Instant instant) throws RelationNotPossibleException, AuthorizationUnavailableException,
        AuthorizationException {

      checkIfAllowedToWrite(authorizer, userId, collection);

      RelationType descs = getRelationDescription(traversal, typeId)
        .orElseThrow(notPossible("Relation type " + typeId + " does not exist"));
      Vertex sourceV = getEntityByFullIteration(traversal, sourceId).orElseThrow(notPossible("source is not present"));
      Vertex targetV = getEntityByFullIteration(traversal, targetId).orElseThrow(notPossible("target is not present"));

      //check if the relation already exists
      final Optional<EntityRelation> existingEdgeOpt = getEntityRelation(sourceV, targetV, typeId, collection);

      if (existingEdgeOpt.isPresent()) {
        final EntityRelation existingEdge = existingEdgeOpt.get();
        if (!existingEdge.isAccepted()) {
          //if not already an active relation
          updateRelation(existingEdge, collection, userId, true, instant);
        }
        return existingEdge.getTimId();
      } else {
        Collection sourceCollection = getCollection(collection.getVre(), sourceV)
          .orElseThrow(notPossible("Source vertex is not part of the VRE of " + collection.getCollectionName()));
        Collection targetCollection = getCollection(collection.getVre(), targetV)
          .orElseThrow(notPossible("Target vertex is not part of the VRE of " + collection.getCollectionName()));
        RelationType.DirectionalRelationType desc = descs.getForDirection(sourceCollection, targetCollection)
                                                         .orElseThrow(notPossible(
                                                           "You can't have a " + descs.getName() + " from " +
                                                             sourceCollection.getEntityTypeName() + " to " +
                                                             targetCollection.getEntityTypeName() + " or vice versa"));

        return createRelation(sourceV, targetV, desc, userId, collection, true, instant);
      }
    }

    private Supplier<RelationNotPossibleException> notPossible(String message) {
      return () -> new RelationNotPossibleException(message);
    }

    public UUID createEntity(Collection col, Optional<Collection> baseCollection, ObjectNode input, String userId,
                             Instant creationTime)
      throws IOException, AuthorizationUnavailableException, AuthorizationException {

      checkIfAllowedToWrite(authorizer, userId, col);

      Map<String, LocalProperty> mapping = col.getWriteableProperties();
      Map<String, LocalProperty> baseMapping = baseCollection.isPresent() ?
        baseCollection.get().getWriteableProperties() : Maps.newHashMap();

      UUID id = UUID.randomUUID();

      GraphTraversal<Vertex, Vertex> traversalWithVertex = traversal.addV();

      Vertex vertex = traversalWithVertex.next();

      Iterator<String> fieldNames = input.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        if (!fieldName.startsWith("@") && !fieldName.startsWith("^") && !fieldName.equals("_id")) {
          if (mapping.containsKey(fieldName)) {
            try {
              mapping.get(fieldName).setJson(vertex, input.get(fieldName));
            } catch (IOException e) {
              throw new IOException(fieldName + " could not be saved. " + e.getMessage(), e);
            }
          } else {
            throw new IOException(String.format("Items of %s have no property %s", col.getCollectionName(), fieldName));
          }

          if (baseMapping.containsKey(fieldName)) {
            try {
              baseMapping.get(fieldName).setJson(vertex, input.get(fieldName));
            } catch (IOException e) {
              LOG.error(configurationFailure, "Field could not be parsed by Admin VRE converter {}_{}",
                baseCollection.get().getCollectionName(), fieldName);
            }
          }
        }
      }

      setAdministrativeProperties(col, userId, creationTime, id, vertex);

      listener.onCreate(vertex);

      duplicateVertex(traversal, vertex);
      return id;
    }


    /*******************************************************************************************************************
     * Support methods:
     ******************************************************************************************************************/
    private Optional<Collection> getCollection(Vre vre, Element sourceV) {
      String ownType = vre.getOwnType(getEntityTypes(sourceV));
      if (ownType == null) {
        return Optional.empty();
      }
      return Optional.of(vre.getCollectionForTypeName(ownType));
    }

    private Optional<EntityRelation> getEntityRelation(Vertex sourceV, Vertex targetV, UUID typeId,
                                                       Collection collection) {
      return stream(sourceV.edges(Direction.BOTH))
        .filter(e ->
          (e.inVertex().id().equals(targetV.id()) || e.outVertex().id().equals(targetV.id())) &&
            getRequiredProp(e, "typeId", "").equals(typeId.toString())
        )
        //sort by rev (ascending)
        .sorted((o1, o2) -> getRequiredProp(o1, "rev", -1).compareTo(getRequiredProp(o2, "rev", -1)))
        //get last element, i.e. with the highest rev, i.e. the most recent
        .reduce((o1, o2) -> o2)
        .map(edge -> makeEntityRelation(edge, collection));
    }

    private void updateRelation(EntityRelation existingEdge, Collection collection, String userId, boolean accepted,
                                Instant time) throws AuthorizationUnavailableException, AuthorizationException {
      final Edge origEdge = getExpectedEdge(traversal, existingEdge.getTimId().toString());
      final Edge newEdge = EdgeManipulator.duplicateEdge(origEdge);
      newEdge.property(collection.getEntityTypeName() + "_accepted", accepted);
      newEdge.property("rev", existingEdge.getRevision() + 1);
      setModified(newEdge, userId, time);
    }

    private UUID createRelation(Vertex source, Vertex target, RelationType.DirectionalRelationType relationType,
                                String userId, Collection collection, boolean accepted, Instant time) throws
      AuthorizationException, AuthorizationUnavailableException {
      UUID id = UUID.randomUUID();
      Edge edge = source.addEdge(
        relationType.getDbName(),
        target,
        collection.getEntityTypeName() + "_accepted", accepted,
        "types", jsnA(
          jsn(collection.getEntityTypeName()),
          jsn(collection.getAbstractType())
        ).toString(),
        "typeId", relationType.getTimId(),
        "tim_id", id.toString(),
        "isLatest", true,
        "rev", 1
      );
      setCreated(edge, userId, time);
      return id;
    }

    private void setAdministrativeProperties(Collection col, String userId, Instant creationTime, UUID id,
                                             Vertex vertex) {
      vertex.property("tim_id", id.toString());
      vertex.property("rev", 1);
      vertex.property("types", String.format(
        "[\"%s\", \"%s\"]",
        col.getEntityTypeName(),
        col.getAbstractType()
      ));

      setCreated(vertex, userId, creationTime);
    }

    private void setCreated(Element element, String userId, Instant instant) {
      final String value = jsnO(
        "timeStamp", jsn(instant.toEpochMilli()),
        "userId", jsn(userId)
      ).toString();
      element.property("created", value);
      element.property("modified", value);
    }

    private void setModified(Element element, String userId, Instant instant) {
      final String value = jsnO(
        "timeStamp", jsn(instant.toEpochMilli()),
        "userId", jsn(userId)
      ).toString();
      element.property("modified", value);
    }

    private static UUID asUuid(String input, Element source) {
      try {
        return UUID.fromString(input);
      } catch (IllegalArgumentException e) {
        LOG.error(databaseInvariant, "wrongly formatted UUID as tim_id: " + input + " on " +
          source.id());
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
      }
    }

    private static EntityRelation makeEntityRelation(Edge edge, Collection collection) {
      final String acceptedPropName = collection.getEntityTypeName() + "_accepted";

      return ImmutableEntityRelation.builder()
                                    .isAccepted(getRequiredProp(edge, acceptedPropName, false))
                                    .timId(asUuid(getRequiredProp(edge, "tim_id", ""), edge))
                                    .revision(getRequiredProp(edge, "rev", -1))
                                    .build();
    }

    @SuppressWarnings("unchecked")
    public static <V> V getRequiredProp(final Element element, final String key, V valueOnException) {
      try {
        Iterator<? extends Property<Object>> revProp = element.properties(key);
        if (revProp.hasNext()) {
          Object value = revProp.next().value();
          return (V) valueOnException.getClass().cast(value);
        } else {
          LOG.error(databaseInvariant, "Value is missing for property " + key + " on element with id " + element.id());
          return valueOnException;
        }
      } catch (RuntimeException e) {
        LOG.error(databaseInvariant, "Something went wrong while getting the property " + key + " from the element " +
          "with id " + (element != null ? element.id() : "<NULL>") + ": " + e.getMessage());
        return valueOnException;
      }
    }

    private static Optional<Vertex> getEntityByFullIteration(GraphTraversalSource traversal, UUID id) {
      return getFirst(traversal
        .V()
        .has("tim_id", id.toString())
        .not(__.has("deleted", true))
        .has("isLatest", true));
    }

    private static <T> Optional<T> getFirst(Traversal<?, T> traversal) {
      if (traversal.hasNext()) {
        return Optional.of(traversal.next());
      } else {
        return Optional.empty();
      }
    }

    private static Edge getExpectedEdge(GraphTraversalSource traversal, String timId) {
      GraphTraversal<Edge, Edge> edge = traversal.E().has("tim_id", timId);
      if (edge.hasNext()) {
        return edge.next();
      } else {
        throw new ObjectSuddenlyDisappearedException("The code assumes that the edge with id " + timId + " is " +
          "available, but it isn't!");
      }
    }

    public static String[] getEntityTypes(Element element) {
      try {
        String typesProp = getRequiredProp(element, "types", "");
        if (typesProp.equals("[]")) {
          LOG.error(databaseInvariant, "Entitytypes not presen on vertex with ID " + element.id());
          return new String[0];
        } else {
          return arrayToEncodedArray.tinkerpopToJava(typesProp, String[].class);
        }
      } catch (IOException e) {
        LOG.error(databaseInvariant, "Could not parse entitytypes property on vertex with ID " + element.id());
        return new String[0];
      }
    }

    private static Optional<RelationType> getRelationDescription(GraphTraversalSource traversal, UUID typeId) {
      return getFirst(traversal
        .V()
        //.has(T.label, LabelP.of("relationtype"))
        .has("tim_id", typeId.toString())
      )
        .map(RelationType::new);
    }

    private static void checkIfAllowedToWrite(Authorizer authorizer, String userId, Collection collection) throws
      AuthorizationException, AuthorizationUnavailableException {
      if (!authorizer.authorizationFor(collection, userId).isAllowedToWrite()) {
        throw AuthorizationException.notAllowedToCreate(collection.getCollectionName());
      }
    }
  }
}
