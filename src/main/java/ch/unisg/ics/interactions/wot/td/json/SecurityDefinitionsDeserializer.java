package ch.unisg.ics.interactions.wot.td.json;

import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.TokenBasedSecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SecurityDefinitionsDeserializer extends JsonDeserializer<Map<String, SecurityScheme>> {

  @Override
  public Map<String, SecurityScheme> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode securityDefinitionsNode = p.getCodec().readTree(p);

    Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
    if (securityDefinitionsNode != null) {
      for (JsonNode element : securityDefinitionsNode) {
        final var schemeType = element.get("@type").get(0).asText();
        final var in =  element.get(WoTSec.in).get(0).get("@value").asText();
        final var name = element.get(WoTSec.name).get(0).get("@value").asText();
        final var instanceName = element.get("https://www.w3" +
                ".org/2019/wot/td#hasInstanceConfiguration").get(0).get("@id").asText();

        SecurityScheme scheme;
        switch (schemeType) {
          case WoTSec.APIKeySecurityScheme:
            Map<String, Object> config = new HashMap<>();
            if (!in.isEmpty()) {
              TokenBasedSecurityScheme.TokenLocation tokenLocation =
                  TokenBasedSecurityScheme.TokenLocation.fromString(in);
              config.put(WoTSec.in, tokenLocation);
            }
            if (!name.isEmpty()) {
              config.put(WoTSec.name, name);
            }
            scheme = new APIKeySecurityScheme(config);
            break;
          case WoTSec.NoSecurityScheme:
            scheme = SecurityScheme.getNoSecurityScheme();
            break;
          default:
            throw new IllegalArgumentException("Unknown security scheme type: " + schemeType);
        }
        securityDefinitions.put("basic_sc", scheme);
      }
    }

    return securityDefinitions;
  }
}
