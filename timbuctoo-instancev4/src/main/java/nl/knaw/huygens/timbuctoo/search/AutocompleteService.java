package nl.knaw.huygens.timbuctoo.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import nl.knaw.huygens.timbuctoo.crud.InvalidCollectionException;
import nl.knaw.huygens.timbuctoo.crud.UrlGenerator;
import nl.knaw.huygens.timbuctoo.logging.Logmarkers;
import nl.knaw.huygens.timbuctoo.model.LocationNames;
import nl.knaw.huygens.timbuctoo.model.PersonNames;
import nl.knaw.huygens.timbuctoo.model.TempName;
import nl.knaw.huygens.timbuctoo.database.dto.dataset.Collection;
import nl.knaw.huygens.timbuctoo.model.vre.Vres;
import nl.knaw.huygens.timbuctoo.search.description.PropertyDescriptor;
import nl.knaw.huygens.timbuctoo.search.description.property.PropertyDescriptorFactory;
import nl.knaw.huygens.timbuctoo.search.description.property.WwDocumentDisplayNameDescriptor;
import nl.knaw.huygens.timbuctoo.search.description.propertyparser.PropertyParserFactory;
import nl.knaw.huygens.timbuctoo.server.TinkerpopGraphManager;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.slf4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nl.knaw.huygens.timbuctoo.model.GraphReadUtils.getProp;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsn;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnA;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnO;

public class AutocompleteService {

  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AutocompleteService.class);

  private final TinkerpopGraphManager graphManager;
  private final Map<String, PropertyDescriptor> displayNameDescriptors;
  private final UrlGenerator autoCompleteUrlFor;
  private final Vres mappings;

  public AutocompleteService(TinkerpopGraphManager graphManager, UrlGenerator autoCompleteUrlFor, Vres mappings) {
    final PropertyDescriptorFactory propertyDescriptorFactory =
            new PropertyDescriptorFactory(new PropertyParserFactory());

    this.graphManager = graphManager;
    this.autoCompleteUrlFor = autoCompleteUrlFor;
    this.mappings = mappings;

    displayNameDescriptors = Maps.newHashMap();
    displayNameDescriptors.put("wwdocuments", new WwDocumentDisplayNameDescriptor());
    displayNameDescriptors.put("wwpersons", propertyDescriptorFactory.getComposite(
            propertyDescriptorFactory.getLocal("wwperson_names", PersonNames.class),
            propertyDescriptorFactory.getLocal("wwperson_tempName", TempName.class)));
    displayNameDescriptors.put("wwkeywords", propertyDescriptorFactory.getLocal("wwkeyword_value", String.class));
    displayNameDescriptors.put("wwlanguages", propertyDescriptorFactory.getLocal("wwlanguage_name", String.class));
    displayNameDescriptors.put("wwlocations", propertyDescriptorFactory.getLocal("names", LocationNames.class));
    displayNameDescriptors.put("wwcollectives", propertyDescriptorFactory.getLocal("wwcollective_name", String.class));
  }

  public JsonNode search(String collectionName, Optional<String> query, Optional<String> type)
          throws InvalidCollectionException {

    final GraphDatabaseService graphDatabase =   graphManager.getGraphDatabase();

    Transaction transaction = graphManager.getGraph().tx();

    // Apparently a lucene search needs a transaction to be open:
    // http://stackoverflow.com/questions/19428017
    if (!transaction.isOpen()) {
      transaction.open();
    }

    if (!graphDatabase.index().existsForNodes(collectionName)) {
      transaction.close();
      return searchGremlin(collectionName, query, type);
    }

    final Index<Node> index = graphDatabase.index().forNodes(collectionName);
    final String parsedQuery = parseQuery(query);

    IndexHits<Node> hits = index.query("displayName", parsedQuery);

    List<ObjectNode> results = StreamSupport.stream(hits.spliterator(), false)
      // FIXME: filtering on the result set is safer (no compound lucene query needed),
      // FIXME: however, it only works when it is sure the result set contains all results (like for keywords)
      .filter(hit -> !(type.isPresent() && !type.get().equals(hit.getProperty("keyword_type"))))
      .map(hit -> {
        Vertex vertex = graphManager.getGraph().traversal().V(hit.getId()).next();
        String timId = (String) vertex.property("tim_id").value();
        int rev = (Integer) vertex.property("rev").value();

        return jsnO(
              "key", jsn(autoCompleteUrlFor.apply(collectionName, UUID.fromString(timId), rev).toString()),
              "value", jsn(displayNameDescriptors.get(collectionName).get(vertex)));
      })
      .limit(collectionName.equals("wwkeywords") ? 1000 : 50) // FIXME: expose param to client again
      .collect(Collectors.toList());

    hits.close();
    transaction.close();
    return jsnA(results.stream());
  }

  private JsonNode searchGremlin(String collectionName, Optional<String> tokenParam, Optional<String> type)
    throws InvalidCollectionException {

    final Collection collection = mappings.getCollection(collectionName)
                                          .orElseThrow(() -> new InvalidCollectionException(collectionName));
    String entityTypeName = collection.getEntityTypeName();
    final GraphTraversal<Vertex, Vertex> traversalSource = graphManager.getCurrentEntitiesFor(entityTypeName);

    GraphTraversal<Vertex, Vertex> typeFilter;
    if (type.isPresent()) {
      typeFilter = __.has("keyword_type", type.get());
    } else {
      typeFilter = __.identity();
    }

    List<ObjectNode> results;
    if (tokenParam.isPresent()) {
      String token = tokenParam.get();
      if (token.startsWith("*")) {
        token = token.substring(1);
      }
      if (token.endsWith("*")) {
        token = token.substring(0, token.length() - 1);
      }
      final String searchToken = token.toLowerCase();

      results = traversalSource.as("vertex")
                               .where(typeFilter)
                               .union(collection.getDisplayName().traversalJson())
                               .filter(x -> x.get().isSuccess())
                               .map(x -> x.get().get().asText())
                               .as("displayName")
                               .filter(x -> x.get().toLowerCase().contains(searchToken))
                               .select("vertex", "displayName")
                               .map(x -> {
                                 Vertex vertex = (Vertex) x.get().get("vertex");
                                 String dn = (String) x.get().get("displayName");
                                 Optional<String> id = getProp(vertex, "tim_id", String.class);
                                 Integer rev = getProp(vertex, "rev", Integer.class).orElse(1);
                                 if (id.isPresent()) {
                                   try {
                                     UUID uuid = UUID.fromString(id.get());
                                     URI uri = autoCompleteUrlFor.apply(collection.getCollectionName(), uuid, rev);
                                     return jsnO(
                                       "key", jsn(uri.toString()),
                                       "value", jsn(dn)
                                     );
                                   } catch (IllegalArgumentException e) {
                                     LOG.error(Logmarkers.databaseInvariant, "Tim_id " + id + "is not a valid UUID");
                                     return null;
                                   }
                                 } else {
                                   LOG.error(Logmarkers.databaseInvariant,
                                     "No Tim_id found on vertex with id " + vertex.id());
                                   return null;
                                 }
                               })
                               .filter(x -> x != null)
                               .limit(50L)
                               .toList();
    } else {
      results = traversalSource.as("vertex")
                               .where(typeFilter)
                               .union(collection.getDisplayName().traversalJson())
                               .filter(x -> x.get().isSuccess())
                               .map(x -> x.get().get().asText())
                               .as("displayName")
                               .select("vertex", "displayName")
                               .map(x -> {
                                 Vertex vertex = (Vertex) x.get().get("vertex");
                                 String dn = (String) x.get().get("displayName");
                                 Optional<String> id = getProp(vertex, "tim_id", String.class);
                                 Integer rev = getProp(vertex, "rev", Integer.class).orElse(1);
                                 if (id.isPresent()) {
                                   try {
                                     UUID uuid = UUID.fromString(id.get());
                                     URI uri = autoCompleteUrlFor.apply(collection.getCollectionName(), uuid, rev);
                                     return jsnO(
                                       "key", jsn(uri.toString()),
                                       "value", jsn(dn)
                                     );
                                   } catch (IllegalArgumentException e) {
                                     LOG.error(Logmarkers.databaseInvariant, "Tim_id " + id + "is not a valid UUID");
                                     return null;
                                   }
                                 } else {
                                   LOG.error(Logmarkers.databaseInvariant,
                                     "No Tim_id found on vertex with id " + vertex.id());
                                   return null;
                                 }
                               })
                               .filter(x -> x != null)
                               .limit(1000L) //no query means you get a lot of results
                               .toList();
    }

    return jsnA(results.stream());
  }

  private String parseQuery(Optional<String> queryParam) {
    if (!queryParam.isPresent()) {
      return "*";
    }

    return queryParam.get()
            .replaceAll("^\\*", "")
            .replaceAll("\\s", " AND ");
  }
}