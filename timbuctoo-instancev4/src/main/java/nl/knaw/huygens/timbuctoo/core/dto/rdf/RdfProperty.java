package nl.knaw.huygens.timbuctoo.core.dto.rdf;

import nl.knaw.huygens.timbuctoo.core.dto.property.TimProperty;
import nl.knaw.huygens.timbuctoo.rdf.conversion.RdfPropertyConverterFactory;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.IOException;

public class RdfProperty {
  private final String predicateUri;
  private final String value;
  private final String typeUri;
  private final RdfPropertyConverterFactory.RdfPropertyConverter converter;

  public RdfProperty(String predicateUri, String value, String typeUri) {
    this.predicateUri = predicateUri;
    this.value = value;
    this.typeUri = typeUri;
    converter = new RdfPropertyConverterFactory().getConverter(predicateUri, typeUri);
  }


  public String getPredicateUri() {
    return predicateUri;
  }

  public String getValue() {
    return value;
  }

  public String getTypeUri() {
    return typeUri;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }

  public TimProperty<?> getTimProperty() throws IOException {
    return converter.convert(value);
  }
}
