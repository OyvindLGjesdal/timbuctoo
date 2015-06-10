package nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop.conversion;

import java.lang.reflect.Field;

import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.storage.graph.ConversionException;
import nl.knaw.huygens.timbuctoo.storage.graph.FieldType;

import com.tinkerpop.blueprints.Element;

public interface PropertyConverter {

  void setField(Field field);

  void setContainingType(Class<? extends Entity> type);

  void setFieldType(FieldType fieldType);

  void setFieldName(String fieldName);

  void setPropertyOfElement(Element element, Entity entity) throws ConversionException;

  void addValueToEntity(Entity entity, Element element) throws ConversionException;

  String propertyName();

  String getFieldName();

  FieldType getFieldType();

  /**
   * Removes the property from the element.
   * @param element the element to remove the property from
   */
  void removeFrom(Element element);
}