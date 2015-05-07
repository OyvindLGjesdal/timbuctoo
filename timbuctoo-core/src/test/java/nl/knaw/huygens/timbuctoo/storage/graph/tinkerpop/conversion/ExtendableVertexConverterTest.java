package nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop.conversion;

import static nl.knaw.huygens.timbuctoo.storage.graph.SubADomainEntityBuilder.aDomainEntity;
import static nl.knaw.huygens.timbuctoo.storage.graph.tinkerpop.ElementFields.ELEMENT_TYPES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import nl.knaw.huygens.timbuctoo.config.TypeNames;
import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.storage.graph.ConversionException;
import nl.knaw.huygens.timbuctoo.storage.graph.EntityInstantiator;
import nl.knaw.huygens.timbuctoo.storage.graph.FieldType;

import org.junit.Before;
import org.junit.Test;

import test.model.BaseDomainEntity;
import test.model.projecta.SubADomainEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

public class ExtendableVertexConverterTest {
  private static final String PROPERTY1_NAME = "property1Name";
  private static final String PROPERTY2_NAME = "property2Name";
  private static final String FIELD1_NAME = "field1Name";
  private static final String FIELD2_NAME = "field2Name";
  private static final Class<BaseDomainEntity> BASE_DOMAIN_ENTITY_TYPE = BaseDomainEntity.class;
  private static final Class<SubADomainEntity> DOMAIN_ENTITY_TYPE = SubADomainEntity.class;
  private PropertyConverter propertyConverter1;
  private PropertyConverter propertyConverter2;
  private List<PropertyConverter> propertyConverters;
  private ExtendableVertexConverter<SubADomainEntity> instance;
  private Vertex vertexMock;
  private SubADomainEntity entity;
  private EntityInstantiator entityInstantiatorMock;
  private PropertyConverter modifiedConverterMock;
  private PropertyConverter revConverterMock;

  @Before
  public void setup() {
    propertyConverter1 = createPropertyConverter(PROPERTY1_NAME, FIELD1_NAME, FieldType.REGULAR);
    propertyConverter2 = createPropertyConverter(PROPERTY2_NAME, FIELD2_NAME, FieldType.REGULAR);
    modifiedConverterMock = createPropertyConverter(Entity.MODIFIED_PROPERTY_NAME, Entity.MODIFIED_PROPERTY_NAME, FieldType.ADMINISTRATIVE);
    revConverterMock = createPropertyConverter(Entity.REVISION_PROPERTY_NAME, Entity.REVISION_PROPERTY_NAME, FieldType.ADMINISTRATIVE);
    propertyConverters = Lists.newArrayList(propertyConverter1, propertyConverter2, modifiedConverterMock, revConverterMock);

    entityInstantiatorMock = mock(EntityInstantiator.class);

    instance = createInstance(DOMAIN_ENTITY_TYPE, propertyConverters);

    vertexMock = mock(Vertex.class);
    entity = new SubADomainEntity();
  }

  private <T extends Entity> ExtendableVertexConverter<T> createInstance(Class<T> type, List<PropertyConverter> propertyConverters) {
    return new ExtendableVertexConverter<T>(type, propertyConverters, entityInstantiatorMock);
  }

  private PropertyConverter createPropertyConverter(String propertyName, String fieldName, FieldType fieldType) {
    PropertyConverter propertyConverter = mock(PropertyConverter.class);
    when(propertyConverter.propertyName()).thenReturn(propertyName);
    when(propertyConverter.getFieldName()).thenReturn(fieldName);
    when(propertyConverter.getFieldType()).thenReturn(fieldType);
    return propertyConverter;
  }

  private <T extends Entity> ExtendableVertexConverter<T> createInstance(Class<T> type) {
    return createInstance(type, propertyConverters);
  }

  @Test
  public void addValuesToVertexLetsThePropertyConvertersAddTheirValuesToTheNode() throws Exception {
    // action
    instance.addValuesToElement(vertexMock, entity);

    // verify
    verify(propertyConverter1).setPropertyOfElement(vertexMock, entity);
    verify(propertyConverter2).setPropertyOfElement(vertexMock, entity);
    verify(modifiedConverterMock).setPropertyOfElement(vertexMock, entity);
    verify(revConverterMock).setPropertyOfElement(vertexMock, entity);
  }

  @Test
  public void addValuesToVertexAddsTheTypeOfTheVertexConverter() throws Exception {
    // action

    ExtendableVertexConverter<BaseDomainEntity> instance = createInstance(BASE_DOMAIN_ENTITY_TYPE);

    instance.addValuesToElement(vertexMock, aDomainEntity().build());

    // verify
    verifyTypeIsSet(vertexMock, BASE_DOMAIN_ENTITY_TYPE);

  }

  private void verifyTypeIsSet(Element elementMock, Class<? extends Entity> type) throws Exception {
    List<String> types = Lists.newArrayList(TypeNames.getInternalName(type));

    ObjectMapper objectMapper = new ObjectMapper();
    String value = objectMapper.writeValueAsString(types);

    verify(elementMock).setProperty(ELEMENT_TYPES, value);
  }

  @Test(expected = ConversionException.class)
  public void addValuesToVertexThrowsAConversionExceptionWhenOneOfThePropertyConvertersDoes() throws Exception {
    // setup
    doThrow(ConversionException.class).when(propertyConverter1).setPropertyOfElement(vertexMock, entity);

    try {
      // action
      instance.addValuesToElement(vertexMock, entity);
    } finally {
      // verify
      verify(propertyConverter1).setPropertyOfElement(vertexMock, entity);
    }
  }

  @Test
  public void convertToEntityCreatesAnInstanceOfTheEntityThenLetThePropertyConvertersAddTheValues() throws Exception {
    // setup
    when(entityInstantiatorMock.createInstanceOf(DOMAIN_ENTITY_TYPE)).thenReturn(entity);

    // action
    SubADomainEntity createdEntity = instance.convertToEntity(vertexMock);

    // verify
    assertThat(createdEntity, is(sameInstance(entity)));

    verify(propertyConverter1).addValueToEntity(entity, vertexMock);
    verify(propertyConverter2).addValueToEntity(entity, vertexMock);

  }

  @Test(expected = ConversionException.class)
  public void convertToEntityThrowsAConversionExceptionWhenTheEntityCannotBeInstatiated() throws Exception {
    // setup
    when(entityInstantiatorMock.createInstanceOf(DOMAIN_ENTITY_TYPE)).thenThrow(new InstantiationException());

    // action
    instance.convertToEntity(vertexMock);

  }

  @Test(expected = ConversionException.class)
  public void convertToEntityThrowsAConversionExceptionWhenOneOfTheValuesCannotBeConverted() throws Exception {
    // setup
    when(entityInstantiatorMock.createInstanceOf(DOMAIN_ENTITY_TYPE)).thenReturn(entity);
    doThrow(ConversionException.class).when(propertyConverter1).addValueToEntity(entity, vertexMock);

    // action
    instance.convertToEntity(vertexMock);
  }

  @Test
  public void convertToSubTypeCreatesAnInstanceOfTheUsedTypeAndAddsThePropertyValuesOfTheTypeOfTheNodeConverter() throws Exception {
    // setup
    SubADomainEntity domainEntity = aDomainEntity().build();
    when(entityInstantiatorMock.createInstanceOf(DOMAIN_ENTITY_TYPE)).thenReturn(domainEntity);

    ExtendableVertexConverter<BaseDomainEntity> instance = createInstance(BASE_DOMAIN_ENTITY_TYPE);

    // action
    SubADomainEntity createdEntity = instance.convertToSubType(DOMAIN_ENTITY_TYPE, vertexMock);

    // verify
    assertThat(createdEntity, is(sameInstance(domainEntity)));

    verify(propertyConverter1).addValueToEntity(domainEntity, vertexMock);
    verify(propertyConverter2).addValueToEntity(domainEntity, vertexMock);
  }

  @Test(expected = ConversionException.class)
  public void convertToSubTypeThrowsAConversionExceptionExceptionWhenTheTypeCannotBeInstantiated() throws Exception {
    when(entityInstantiatorMock.createInstanceOf(DOMAIN_ENTITY_TYPE)).thenThrow(new InstantiationException());

    ExtendableVertexConverter<BaseDomainEntity> instance = createInstance(BASE_DOMAIN_ENTITY_TYPE);

    instance.convertToSubType(DOMAIN_ENTITY_TYPE, vertexMock);
  }

  @Test(expected = ConversionException.class)
  public void convertToSubTypeThrowsAConverterExceptionWhenOneOfTheFieldsCannotBeConverted() throws Exception {
    // setup
    SubADomainEntity domainEntity = aDomainEntity().build();
    when(entityInstantiatorMock.createInstanceOf(DOMAIN_ENTITY_TYPE)).thenReturn(domainEntity);

    doThrow(ConversionException.class).when(propertyConverter1).addValueToEntity(domainEntity, vertexMock);

    ExtendableVertexConverter<BaseDomainEntity> instance = createInstance(BASE_DOMAIN_ENTITY_TYPE);

    // action
    instance.convertToSubType(DOMAIN_ENTITY_TYPE, vertexMock);
  }

  @Test
  public void getPropertyNameReturnsTheNameOfTheFoundPropertyConverter() {
    // setup
    when(propertyConverter1.propertyName()).thenReturn(PROPERTY1_NAME);

    // action
    String actualPropertyName = instance.getPropertyName(FIELD1_NAME);

    // verify
    assertThat(actualPropertyName, is(equalTo(PROPERTY1_NAME)));
  }

  @Test(expected = FieldNonExistingException.class)
  public void getPropertyNameThrowsARuntimeExceptionWhenThePropertyIsNotFound() {
    // setup
    String nonExistingFieldName = "nonExistingPropertyName";

    // action
    instance.getPropertyName(nonExistingFieldName);
  }

  @Test
  public void updateModifiedAndRevLetTheFieldConvertersSetTheValuesForRevisionAndModified() throws Exception {
    // action
    instance.updateModifiedAndRev(vertexMock, entity);

    // verify
    verify(modifiedConverterMock).setPropertyOfElement(vertexMock, entity);
    verify(revConverterMock).setPropertyOfElement(vertexMock, entity);
    verify(propertyConverter1, times(0)).setPropertyOfElement(vertexMock, entity);
    verify(propertyConverter2, times(0)).setPropertyOfElement(vertexMock, entity);
  }

  @Test(expected = ConversionException.class)
  public void updateModifiedAndRevThrowsAConversionExceptionIfOneOfTheFieldsCannotBeConverted() throws Exception {
    // setup
    doThrow(ConversionException.class).when(modifiedConverterMock).setPropertyOfElement(vertexMock, entity);

    // action
    instance.updateModifiedAndRev(vertexMock, entity);
  }

  @Test
  public void updateVertexSetsTheValuesOfTheNonAdministrativeFields() throws Exception {

    // action
    instance.updateElement(vertexMock, entity);

    // verify
    verify(propertyConverter1).setPropertyOfElement(vertexMock, entity);
    verify(propertyConverter2).setPropertyOfElement(vertexMock, entity);

    verify(modifiedConverterMock, times(0)).setPropertyOfElement(vertexMock, entity);
    verify(revConverterMock, times(0)).setPropertyOfElement(vertexMock, entity);
  }

  @Test(expected = ConversionException.class)
  public void updateVertexThrowsAConversionExceptionWhenAFieldConverterThrowsOne() throws Exception {
    // setup
    doThrow(ConversionException.class).when(propertyConverter1).setPropertyOfElement(vertexMock, entity);

    // action
    instance.updateElement(vertexMock, entity);
  }
}
