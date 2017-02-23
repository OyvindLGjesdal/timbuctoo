package nl.knaw.huygens.timbuctoo.server.endpoints.v2.domain;

import io.dropwizard.jersey.params.UUIDParam;
import nl.knaw.huygens.timbuctoo.core.NotFoundException;
import nl.knaw.huygens.timbuctoo.core.TransactionEnforcer;
import nl.knaw.huygens.timbuctoo.core.dto.ReadEntity;
import nl.knaw.huygens.timbuctoo.core.dto.dataset.Collection;
import nl.knaw.huygens.timbuctoo.crud.InvalidCollectionException;
import nl.knaw.huygens.timbuctoo.rdf.LiteralTriple;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static nl.knaw.huygens.timbuctoo.core.TransactionStateAndResult.commitAndReturn;

@Path("/v2.1/domain/{collection}/{id}")
@Produces("application/n-triples")
public class SingleEntityNTriple {
  private final TransactionEnforcer transactionEnforcer;

  public SingleEntityNTriple(TransactionEnforcer transactionEnforcer) {
    this.transactionEnforcer = transactionEnforcer;
  }

  @GET
  public Response get(@PathParam("collection") String collectionName,
                      @PathParam("id") UUIDParam id,
                      @QueryParam("rev") Integer rev
  ) {
    return transactionEnforcer.executeAndReturn(timbuctooActions -> {
      try {
        Collection collection = timbuctooActions.getCollectionMetadata(collectionName);
        ReadEntity entity = timbuctooActions.getEntity(collection, id.get(), rev);
        URI rdfUri = entity.getRdfUri();
        String rdfString = rdfUri == null ?
          "http://timbuctoo.huygens.knaw.nl/" + collectionName + "/" + entity.getId() :
          rdfUri.toString();
        StringBuilder sb = new StringBuilder();
        addRdfProp(rdfString, sb, "id", entity.getId());
        entity.getProperties().forEach(prop -> addRdfProp(rdfString, sb, prop.getName(), prop.getValue()));
        return commitAndReturn(Response.ok(sb.toString()).build());
      } catch (InvalidCollectionException e) {
        return commitAndReturn(Response.status(BAD_REQUEST).entity(e.getMessage()).build());
      } catch (NotFoundException e) {
        return commitAndReturn(Response.status(NOT_FOUND).build());
      }
    });
  }

  private void addRdfProp(String rdfString, StringBuilder sb, String propName, Object propValue) {
    sb.append(
      new LiteralTriple(rdfString, String.format("http://timbuctoo.huygens.knaw.nl/%s", propName), propValue)
        .getStringValue());
    sb.append("\n");
  }
}
