package nl.knaw.huygens.timbuctoo.server.search.property;

import nl.knaw.huygens.timbuctoo.server.search.PropertyDescriptor;
import nl.knaw.huygens.timbuctoo.server.search.PropertyParser;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

public class PropertyDescriptorFactoryTest {

  @Test
  public void getLocalWithPropertyNameAndParserReturnsALocalPropertyDescriptor() {
    PropertyDescriptorFactory instance = new PropertyDescriptorFactory();
    String propertyName = "propertyName";
    PropertyParser parser = mock(PropertyParser.class);

    PropertyDescriptor descriptor = instance.getLocal(propertyName, parser);

    assertThat(descriptor, is(instanceOf(LocalPropertyDescriptor.class)));
  }

  @Test
  public void getCompositeReturnsACompositePropertyDescriptor() {
    PropertyDescriptorFactory instance = new PropertyDescriptorFactory();

    PropertyDescriptor descriptor =
      instance.getComposite(mock(PropertyDescriptor.class), mock(PropertyDescriptor.class));

    assertThat(descriptor, is(instanceOf(CompositePropertyDescriptor.class)));
  }


}