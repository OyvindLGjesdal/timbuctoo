package nl.knaw.huygens.timbuctoo.tools.util.metadata;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.common.collect.Maps;

public class ConstantFieldMetaDataGenerator extends FieldMetaDataGenerator {

  public ConstantFieldMetaDataGenerator(TypeFacade containingType) {
    super(containingType);
  }

  @Override
  protected Map<String, Object> constructValue(Field field) {
    Map<String, Object> metaDataMap = Maps.newHashMap();
    metaDataMap.put(TYPE_FIELD, containingType.getTypeNameOfField(field));
    // to get the values of private constants
    field.setAccessible(true);

    try {
      metaDataMap.put(VALUE_FIELD, field.get(null));
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return metaDataMap;
  }
}
