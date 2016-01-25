package nl.knaw.huygens.timbuctoo.server.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nl.knaw.huygens.timbuctoo.server.rest.search.SearchRequestV2_1;
import nl.knaw.huygens.timbuctoo.server.search.LocationNames.LocationType;
import nl.knaw.huygens.timbuctoo.server.search.property.PropertyDescriptorFactory;
import nl.knaw.huygens.timbuctoo.server.search.propertyparser.PropertyParserFactory;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WwPersonSearchDescription {
  public static final Logger LOG = LoggerFactory.getLogger(WwPersonSearchDescription.class);
  private final PropertyParserFactory propertyParserFactory;
  private final PropertyDescriptorFactory propertyDescriptorFactory;
  private ObjectMapper objectMapper;

  private static final List<String> SORTABLE_FIELDS = Lists.newArrayList(
    "dynamic_k_modified",
    "dynamic_k_birthDate",
    "dynamic_sort_name",
    "dynamic_k_deathDate");

  private static final List<String> FULL_TEXT_SEARCH_FIELDS = Lists.newArrayList(
    "dynamic_t_tempspouse",
    "dynamic_t_notes",
    "dynamic_t_name");
  public static final String ID_DB_PROP = "tim_id";
  private final String type = "wwperson";

  public WwPersonSearchDescription() {
    objectMapper = new ObjectMapper();
    propertyParserFactory = new PropertyParserFactory();
    propertyDescriptorFactory = new PropertyDescriptorFactory();
  }

  public List<String> getSortableFields() {
    return SORTABLE_FIELDS;
  }

  public List<String> getFullTextSearchFields() {
    return FULL_TEXT_SEARCH_FIELDS;
  }

  public TimbuctooQuery createQuery(SearchRequestV2_1 searchRequest) {
    return new TimbuctooQuery(this);
  }

  public EntityRef createRef(Vertex vertex) {
    String id = getSinglePropDescriptor(
      new PropertyParserFactory().getParser(String.class), ID_DB_PROP).get(vertex);

    EntityRef ref = new EntityRef(type, id);

    PropertyDescriptor descriptor = propertyDescriptorFactory.getComposite(
      getSinglePropDescriptor(new PropertyParserFactory().getParser(PersonNames.class), "wwperson_names"),
      getSinglePropDescriptor(new PropertyParserFactory().getParser(String.class), "wwperson_tempName"));

    String displayName = descriptor.get(vertex);
    ref.setDisplayName(displayName);

    Map<String, Object> data = Maps.newHashMap();
    data.put("_id", id);
    data.put("name", displayName);

    data.put("birthDate", getSinglePropDescriptor(
      new PropertyParserFactory().getParser(Datable.class), "wwperson_birthDate").get(vertex));
    data.put("deathDate", getSinglePropDescriptor(
      new PropertyParserFactory().getParser(Datable.class), "wwperson_deathDate").get(vertex));
    data.put("gender", getSinglePropDescriptor(
      new PropertyParserFactory().getParser(Gender.class), "wwperson_gender").get(vertex));

    data.put("modified_date",
      getSinglePropDescriptor(
        propertyParserFactory.getParser(Change.class), "modified").get(vertex));
    setResidenceLocation(vertex, data);
    ref.setData(data);

    return ref;
  }

  public PropertyDescriptor getSinglePropDescriptor(PropertyParser parser, String propertyName) {
    return propertyDescriptorFactory.getLocal(propertyName, parser);
  }

  private void setResidenceLocation(Vertex vertex, Map<String, Object> data) {
    Iterator<Vertex> residenceLocations = vertex.vertices(Direction.OUT, "hasResidenceLocation");
    StringBuilder sb = new StringBuilder();
    for (; residenceLocations.hasNext(); ) {
      Vertex location = residenceLocations.next();

      String names = getValueAsString(location, "names");
      String locationType = getValueAsString(location, "locationType");


      if (names != null && locationType != null) {
        if (sb.length() > 0) {
          sb.append(";");
        }
        try {

          LocationNames names1 = objectMapper.readValue(names, LocationNames.class);
          sb.append(names1.getDefaultName(objectMapper.readValue(locationType, LocationType.class)));

        } catch (IOException e) {
          LOG.error("Could not convert 'location name' with value '{}'", names);
          LOG.error("Exception throw", e);
        }
      }
      if (sb.length() > 0) {
        data.put("residenceLocation", sb.toString());
      }
    }
    if (!data.containsKey("residenceLocation")) {
      data.put("residenceLocation", null);
    }
  }

  private String getValueAsString(Vertex vertex, String propertyName) {
    String value = null;
    if (vertex.keys().contains(propertyName)) {
      value = vertex.value(propertyName);
    }
    return value;
  }

  public GraphTraversal<Vertex, Vertex> filterByType(GraphTraversal<Vertex, Vertex> vertices) {
    return vertices.filter(x -> ((String) x.get().property("types").value()).contains(type));
  }

  String getType() {
    return type;
  }

}