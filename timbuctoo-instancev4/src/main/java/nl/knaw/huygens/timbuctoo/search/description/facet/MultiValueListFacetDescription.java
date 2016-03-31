package nl.knaw.huygens.timbuctoo.search.description.facet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import nl.knaw.huygens.timbuctoo.search.FacetValue;
import nl.knaw.huygens.timbuctoo.search.description.FacetDescription;
import nl.knaw.huygens.timbuctoo.search.description.facet.Facet.DefaultOption;
import nl.knaw.huygens.timbuctoo.search.description.facet.Facet.Option;
import nl.knaw.huygens.timbuctoo.search.description.facet.helpers.ListFacetGetter;
import nl.knaw.huygens.timbuctoo.search.description.facet.helpers.MultiValuePropertyGetter;
import nl.knaw.huygens.timbuctoo.server.mediatypes.v2.search.ListFacetValue;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.toList;

class MultiValueListFacetDescription implements FacetDescription {
  private final String facetName;
  private final String propertyName;
  private final ListFacetGetter listFacetGetter;

  public MultiValueListFacetDescription(String facetName, String propertyName) {
    this.facetName = facetName;
    this.propertyName = propertyName;
    this.listFacetGetter = new ListFacetGetter();
  }

  @Override
  public void filter(GraphTraversal<Vertex, Vertex> graphTraversal, List<FacetValue> facetValues) {
    Optional<FacetValue> first = facetValues.stream()
            .filter(facetValue -> Objects.equals(facetValue.getName(), facetName))
            .findFirst();

    if (!first.isPresent()) {
      return;
    }

    FacetValue facetValue = first.get();

    if (!(facetValue instanceof ListFacetValue)) {
      return;
    }
    List<String> values = ((ListFacetValue) facetValue).getValues();
    if (values.isEmpty()) {
      return;
    }

    graphTraversal
            .where(__.<String>has(propertyName, P.test((o1, o2) -> {
              return o1 instanceof String && o2 instanceof List &&
                      ((List<?>) o2).stream().anyMatch(value -> ((String) o1).contains("\"" + value + "\""));
            }, values)));

  }

  @Override
  public String getName() {
    return facetName;
  }

  @Override
  public Facet getFacet(Map<String, Set<Vertex>> values) {
    return listFacetGetter.getFacet(facetName, values);
  }

  @Override
  public List<String> getValues(Vertex vertex) {
    return MultiValuePropertyGetter.getValues(vertex, propertyName);
  }
}
