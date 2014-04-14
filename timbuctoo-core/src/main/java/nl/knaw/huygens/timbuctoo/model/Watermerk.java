package nl.knaw.huygens.timbuctoo.model;

import nl.knaw.huygens.timbuctoo.annotations.IDPrefix;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotation;

@IDPrefix("REGI")
public class Watermerk extends DomainEntity {

  //  private String type;
  private String value;

  @Override
  public String getDisplayName() {
    return value;
  };

  @IndexAnnotation(fieldName = "dynamic_t_value", isFaceted = true)
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
