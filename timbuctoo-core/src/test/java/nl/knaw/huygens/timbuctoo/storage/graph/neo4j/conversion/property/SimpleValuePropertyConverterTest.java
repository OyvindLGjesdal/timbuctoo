package nl.knaw.huygens.timbuctoo.storage.graph.neo4j.conversion.property;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import nl.knaw.huygens.timbuctoo.model.Entity;
import nl.knaw.huygens.timbuctoo.storage.graph.ConversionException;
import nl.knaw.huygens.timbuctoo.storage.graph.FieldType;
import nl.knaw.huygens.timbuctoo.storage.graph.neo4j.conversion.property.SimpleValuePropertyConverter;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

import test.model.TestSystemEntityWrapper;

public class SimpleValuePropertyConverterTest implements PropertyConverterTest {
  private static final String STRING_VALUE = "stringValue";
  private static final Class<TestSystemEntityWrapper> TYPE = TestSystemEntityWrapper.class;
  private static final String FIELD_NAME = STRING_VALUE;
  private static final FieldType FIELD_TYPE = FieldType.REGULAR;
  private SimpleValuePropertyConverter instance;
  private Node nodeMock;
  private Field field;
  private String propertyName;
  private TestSystemEntityWrapper entity;

  @Before
  public void setUp() throws Exception {
    entity = new TestSystemEntityWrapper();
    nodeMock = mock(Node.class);
    propertyName = FIELD_TYPE.propertyName(TYPE, FIELD_NAME);

    field = TYPE.getDeclaredField(FIELD_NAME);
    instance = new SimpleValuePropertyConverter();
    setupInstance(instance);

  }

  private void setupInstance(SimpleValuePropertyConverter simpleValueFieldWrapper) {
    simpleValueFieldWrapper.setField(field);
    simpleValueFieldWrapper.setFieldType(FIELD_TYPE);
    simpleValueFieldWrapper.setName(FIELD_NAME);
    simpleValueFieldWrapper.setContainingType(TYPE);
  }

  @Override
  @Test
  public void addValueToNodeSetsThePropertyWithTheFieldNameToTheValueOfTheNode() throws Exception {
    // setup
    String value = "value";
    entity.setStringValue(value);

    // action
    instance.setPropertyContainerProperty(nodeMock, entity);

    // verify
    verify(nodeMock).setProperty(propertyName, value);
  }

  @Override
  @Test
  public void addValueToNodeDoesNotSetIfTheValueIsNull() throws Exception {
    // setup
    String value = null;
    entity.setStringValue(value);

    // action
    instance.setPropertyContainerProperty(nodeMock, entity);

    // verify
    verify(nodeMock, never()).setProperty(anyString(), any());
  }

  @Test(expected = ConversionException.class)
  @Override
  public void addValueToNodeThrowsAConversionExceptionIfGetFieldValueThrowsAnIllegalAccessException() throws Exception {
    // setup
    SimpleValuePropertyConverter instance = new SimpleValuePropertyConverter() {
      @Override
      protected Object getFieldValue(Entity entity) throws IllegalArgumentException, IllegalAccessException {
        throw new IllegalAccessException();
      }
    };
    setupInstance(instance);

    // action
    instance.setPropertyContainerProperty(nodeMock, entity);
  }

  @Test(expected = ConversionException.class)
  @Override
  public void addValueToNodeThrowsAConversionExceptionIfGetFieldValueThrowsAnIllegalArgumentExceptionIsThrown() throws Exception {
    // setup
    SimpleValuePropertyConverter instance = new SimpleValuePropertyConverter() {
      @Override
      protected Object getFieldValue(Entity entity) throws IllegalArgumentException, IllegalAccessException {
        throw new IllegalArgumentException();
      }
    };
    setupInstance(instance);

    // action
    instance.setPropertyContainerProperty(nodeMock, entity);
  }

  @Test(expected = ConversionException.class)
  @Override
  public void addValueToNodeThrowsAConversionExceptionIfGetFormatedValueThrowsAnIllegalArgumentException() throws Exception {
    String value = "value";
    entity.setStringValue(value);

    // setup
    SimpleValuePropertyConverter instance = new SimpleValuePropertyConverter() {
      @Override
      protected Object getFormattedValue(Object fieldValue) throws IllegalArgumentException {
        throw new IllegalArgumentException();
      }
    };
    setupInstance(instance);

    // action
    instance.setPropertyContainerProperty(nodeMock, entity);
  }

  @Override
  @Test
  public void addValueToEntitySetTheFieldOfTheEntityWithTheValue() throws Exception {
    nodeHasValueFor(propertyName, STRING_VALUE);

    // action
    instance.addValueToEntity(entity, nodeMock);

    // verify
    assertThat(entity.getStringValue(), is(equalTo(STRING_VALUE)));
    verify(nodeMock, atLeastOnce()).hasProperty(propertyName);
    verify(nodeMock).getProperty(propertyName);
    verifyNoMoreInteractions(nodeMock);

  }

  @Override
  @Test
  public void addValueToEntityDoesNothingIfThePropertyDoesNotExist() throws Exception {
    // setup
    when(nodeMock.hasProperty(propertyName)).thenReturn(false);

    // action
    instance.addValueToEntity(entity, nodeMock);

    // verify
    assertThat(entity.getStringValue(), is(nullValue()));
    verify(nodeMock).hasProperty(propertyName);
    verifyNoMoreInteractions(nodeMock);
  }

  @Test(expected = ConversionException.class)
  @Override
  public void addValueToEntityThrowsAConversionExceptionWhenFillFieldThrowsAnIllegalAccessExceptionIsThrown() throws Exception {
    // setup 
    nodeHasValueFor(propertyName, STRING_VALUE);

    SimpleValuePropertyConverter instance = new SimpleValuePropertyConverter() {
      @Override
      protected void fillField(nl.knaw.huygens.timbuctoo.model.Entity entity, PropertyContainer propertyContainer) throws IllegalArgumentException, IllegalAccessException {
        throw new IllegalAccessException();
      }
    };
    setupInstance(instance);

    // action
    instance.addValueToEntity(entity, nodeMock);

  }

  private void nodeHasValueFor(String propertyName, String value) {
    when(nodeMock.hasProperty(propertyName)).thenReturn(true);
    when(nodeMock.getProperty(propertyName)).thenReturn(value);
  }

  @Test(expected = ConversionException.class)
  @Override
  public void addValueToEntityThrowsAConversionExceptionWhenFillFieldThrowsAnAnIllegalArgumentExceptionIsThrown() throws Exception {
    nodeHasValueFor(propertyName, STRING_VALUE);

    SimpleValuePropertyConverter instance = new SimpleValuePropertyConverter() {
      @Override
      protected void fillField(nl.knaw.huygens.timbuctoo.model.Entity entity, PropertyContainer propertyContainer) throws IllegalArgumentException, IllegalAccessException {
        throw new IllegalArgumentException();
      }
    };
    setupInstance(instance);

    //action
    instance.addValueToEntity(entity, nodeMock);
  }

}
