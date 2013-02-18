package nl.knaw.huygens.repository.storage.mongo.variation;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

import net.vz.mongodb.jackson.internal.stream.JacksonDBObject;

import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.model.util.Change;
import nl.knaw.huygens.repository.storage.generic.JsonViews;
import nl.knaw.huygens.repository.storage.generic.StorageConfiguration;
import nl.knaw.huygens.repository.variation.VariationInducer;

public class MongoModifiableVariationStorage extends MongoVariationStorage {

  private VariationInducer inducer;

  public MongoModifiableVariationStorage(StorageConfiguration conf) throws UnknownHostException, MongoException {
    super(conf);
    inducer = new VariationInducer();
    inducer.setView(JsonViews.DBView.class);
  }

  @Override
  public <T extends Document> void addItem(T newItem, Class<T> cls) throws IOException {
    JsonNode jsonNode = inducer.induce(newItem, cls);
    DBCollection col = getRawCollection(cls);
    JacksonDBObject<JsonNode> insertedItem = new JacksonDBObject<JsonNode>(jsonNode, JsonNode.class);
    col.insert(insertedItem);
  }

  @Override
  public <T extends Document> void addItems(List<T> items, Class<T> cls) throws IOException {
    List<JsonNode> jsonNodes = inducer.induce(items, cls, Collections.<String, DBObject>emptyMap());
    DBCollection col = getRawCollection(cls);
    DBObject[] dbObjects = new DBObject[jsonNodes.size()];
    int i = 0;
    for (JsonNode n : jsonNodes) {
      JacksonDBObject<JsonNode> updatedDBObj = new JacksonDBObject<JsonNode>(n, JsonNode.class);
      dbObjects[i++] = updatedDBObj;
    }
    col.insert(dbObjects);
  }

  @Override
  public <T extends Document> void updateItem(String id, T updatedItem, Class<T> cls) throws IOException {
    DBCollection col = getRawCollection(cls);
    BasicDBObject q = new BasicDBObject("_id", id);
    q.put("^rev", updatedItem.getRev());
    DBObject existingNode = col.findOne(q);
    if (existingNode == null) {
      throw new IOException("No document was found for ID " + id + " and revision " + String.valueOf(updatedItem.getRev()) + " !");
    }
    JsonNode updatedNode = inducer.induce(updatedItem, cls, existingNode);
    ((ObjectNode) updatedNode).put("^rev", updatedItem.getRev() + 1);
    JacksonDBObject<JsonNode> updatedDBObj = new JacksonDBObject<JsonNode>(updatedNode, JsonNode.class);
    col.update(q, updatedDBObj);
  }

  @Override
  public <T extends Document> void deleteItem(String id, Class<T> cls, Change change) throws IOException {
    DBCollection col = getRawCollection(cls);
    BasicDBObject up = new BasicDBObject("$inc", new BasicDBObject("^rev", 1));
    up.put("$set", new BasicDBObject("^deleted", true));
    col.update(new BasicDBObject("_id", id), up);
  }

  @Override
  public void empty() {
    db.cleanCursors(true);
    mongo.dropDatabase(dbName);
    db = mongo.getDB(dbName);
  }

  @Override
  public <T extends Document> void removeReference(Class<T> cls, List<String> accessorList, List<String> referringIds, String referredId, Change change) {
    // TODO Auto-generated method stub
    
  }

}
