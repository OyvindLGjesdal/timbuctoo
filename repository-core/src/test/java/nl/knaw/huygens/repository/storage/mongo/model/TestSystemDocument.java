package nl.knaw.huygens.repository.storage.mongo.model;

import java.util.Date;

import nl.knaw.huygens.repository.annotations.IDPrefix;
import nl.knaw.huygens.repository.facet.IndexAnnotation;
import nl.knaw.huygens.repository.model.SystemDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@IDPrefix("TSTD")
public class TestSystemDocument extends SystemDocument {

  private String name;
  private String testValue1;
  private String testValue2;
  @JsonProperty("propAnnotated")
  private String annotatedProperty;
  private String propWithAnnotatedAccessors;
  private Date date;

  @Override
  @JsonProperty("!currentVariation")
  public String getCurrentVariation() {
    return null;
  }

  @Override
  @JsonProperty("!currentVariation")
  public void setCurrentVariation(String currentVariation) {}

  @Override
  @JsonIgnore
  @IndexAnnotation(fieldName = "desc")
  public String getDisplayName() {
    return name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTestValue1() {
    return testValue1;
  }

  public void setTestValue1(String testValue1) {
    this.testValue1 = testValue1;
  }

  public String getTestValue2() {
    return testValue2;
  }

  public void setTestValue2(String testValue2) {
    this.testValue2 = testValue2;
  }

  public String getAnnotatedProperty() {
    return annotatedProperty;
  }

  public void setAnnotatedProperty(String annotatedProperty) {
    this.annotatedProperty = annotatedProperty;
  }

  @JsonProperty("pwaa")
  public String getPropWithAnnotatedAccessors() {
    return propWithAnnotatedAccessors;
  }

  @JsonProperty("pwaa")
  public void setPropWithAnnotatedAccessors(String propWithAnnotatedAccessors) {
    this.propWithAnnotatedAccessors = propWithAnnotatedAccessors;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

}
