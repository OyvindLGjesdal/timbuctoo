package nl.knaw.huygens.timbuctoo.server.endpoints.v2;


import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import nl.knaw.huygens.contractdiff.diffresults.MatchingDiffResult;
import nl.knaw.huygens.contractdiff.diffresults.MisMatchDiffResult;
import nl.knaw.huygens.contractdiff.jsondiff.JsonDiffer;
import nl.knaw.huygens.timbuctoo.server.TimbuctooConfiguration;
import nl.knaw.huygens.timbuctoo.server.TimbuctooV4;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static nl.knaw.huygens.contractdiff.jsondiff.JsonDiffer.jsonDiffer;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsn;
import static nl.knaw.huygens.timbuctoo.util.JsonBuilder.jsnO;

@RunWith(ConcordionRunner.class)
public class RelationTypesV2_1EndpointFixture extends AbstractV2_1EndpointFixture {
  @ClassRule
  public static final DropwizardAppRule<TimbuctooConfiguration> APPLICATION;

  static {
    APPLICATION = new DropwizardAppRule<>(TimbuctooV4.class,
            ResourceHelpers.resourceFilePath("acceptance_test_config.yaml"));
  }

  @Override
  protected JsonDiffer makeJsonDiffer() {
    return jsonDiffer()
            .handleArraysWith(
                    "ALL_MATCH_ONE_OF",
                    expectationVal -> {
                      if (expectationVal.size() > 1) {
                        ObjectNode expectation = jsnO();
                        for (int i = 0; i < expectationVal.size(); i++) {
                          if (expectationVal.get(i).has("type")) {
                            expectation.set(expectationVal.get(i).get("type").asText(), expectationVal.get(i));
                          } else {
                            throw new RuntimeException("Expectation value has no property 'type': " + expectationVal);
                          }
                        }
                        return jsnO(
                                "possibilities", expectation,
                                "keyProp", jsn("type")
                        );
                      } else {
                        return jsnO(
                                "invariant", expectationVal.get(0)
                        );
                      }
                    })
            .withCustomHandler("BOOLEAN", (actual) -> {
              if (actual.getNodeType().equals(JsonNodeType.BOOLEAN)) {
                return new MatchingDiffResult("a boolean", actual.toString());
              } else {
                return new MisMatchDiffResult("a boolean", actual.toString());
              }
            })
            .build();
  }

  @Override
  protected WebTarget returnUrlToMockedOrRealServer(String serverAddress) {
    String defaultAddress = String.format("http://localhost:%d", APPLICATION.getLocalPort());
    String address = serverAddress != null ? serverAddress : defaultAddress;

    return ClientBuilder.newClient().target(address);
  }

}
