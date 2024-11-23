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
      for (Iterator<Map.Entry<String, JsonNode>> it = securityDefinitionsNode.fields(); it.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = it.next();
        String key = entry.getKey();

        JsonNode value = entry.getValue();
        String schemeType = value.get("scheme").asText();

        SecurityScheme scheme;
        switch (schemeType) {
          case "apikey":
            Map<String, Object> config = new HashMap<>();
            if (value.has("in")) {
              String inValue = value.get("in").asText();
              TokenBasedSecurityScheme.TokenLocation tokenLocation =
                  TokenBasedSecurityScheme.TokenLocation.fromString(inValue);
              config.put(WoTSec.in, tokenLocation);
            }
            if (value.has("name")) {
              config.put(WoTSec.name, value.get("name").asText());
            }
            scheme = new APIKeySecurityScheme(config);
            break;
          case "nosec":
            scheme = SecurityScheme.getNoSecurityScheme();
            break;
          default:
            throw new IllegalArgumentException("Unknown security scheme type: " + schemeType);
        }
        securityDefinitions.put(key, scheme);
      }
    }

    return securityDefinitions;
  }
}
