package nl.knaw.huygens.timbuctoo.v5.serializable;

import nl.knaw.huygens.timbuctoo.v5.datastores.prefixstore.TypeNameStore;

import java.io.IOException;

public interface Serialization {

  /**
   * Use this method to write a intro if needed
   */
  void initialize(TocGenerator tocGenerator, TypeNameStore typeNameStore) throws IOException;

  /**
   * Use this method to finish the serialization and write an appendix if needed
   */
  void finish() throws IOException;

  /**
   * This method is called every time a new graphql Object is opened, except for objects that represent wrapped scalars.
   * @param uri the uri of the entity
   */
  void onStartEntity(String uri) throws IOException;

  /**
   * Called every time a new object property starts.
   *
   * <p>Will be followed by an onStartEntity, onStartList, onRdfValue or onValue.
   *
   */
  void onProperty(String propertyName) throws IOException;

  /**
   * Called at the end of an entity
   */
  void onCloseEntity(String uri) throws IOException;

  /**
   * Called when a new list is started
   */
  void onStartList() throws IOException;

  /**
   * Called every time a new list item starts
   *
   * <p>Will be followed by an onStartEntity, onStartList, onRdfValue or onValue.
   */
  void onListItem(int index) throws IOException;

  /**
   * Called when the list is closed
   */
  void onCloseList() throws IOException;

  /**
   * Called for each wrapper {_value, _type} object
   */
  void onRdfValue(Object value, String valueType) throws IOException;

  /**
   * Called for each non-wrapped scalar object (such as those generated by the graphql introspection system)
   */
  void onValue(Object value) throws IOException;

}
