package nl.knaw.huygens.timbuctoo.core.dto.property;

import nl.knaw.huygens.timbuctoo.core.PropertyConverter;
import nl.knaw.huygens.timbuctoo.util.Tuple;

import java.io.IOException;

public class ArrayProperty extends TimProperty<String> {

  public ArrayProperty(String name, String value) {
    super(name, value);
  }

  @Override
  public <TypeT> Tuple<String, TypeT> convert(PropertyConverter<TypeT> propertyConverter) throws IOException {
    return propertyConverter.to(this);
  }
}
