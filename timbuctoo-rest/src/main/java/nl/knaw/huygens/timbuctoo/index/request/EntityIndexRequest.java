package nl.knaw.huygens.timbuctoo.index.request;

import nl.knaw.huygens.timbuctoo.index.IndexException;
import nl.knaw.huygens.timbuctoo.index.Indexer;
import nl.knaw.huygens.timbuctoo.model.DomainEntity;

class EntityIndexRequest extends AbstractIndexRequest {
  private final String id;

  public EntityIndexRequest(Class<? extends DomainEntity> type, String id) {
    super(type);
    this.id = id;
  }

  @Override
  protected void executeIndexAction(Indexer indexer) throws IndexException {
    indexer.executeIndexAction(getType(), id);
  }
}
