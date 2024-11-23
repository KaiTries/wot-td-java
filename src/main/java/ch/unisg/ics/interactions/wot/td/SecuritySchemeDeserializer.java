package ch.unisg.ics.interactions.wot.td;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.TokenBasedSecurityScheme.TokenLocation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SecuritySchemeDeserializer extends JsonDeserializer<SecurityScheme> {

  @Override
  public SecurityScheme deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = p.getCodec().readTree(p);
    JsonNode schemeTypeNode = node.get("scheme");

    if (schemeTypeNode == null) {
      throw new IllegalArgumentException("Missing field: scheme");
    }

    String schemeType = schemeTypeNode.asText();
    Map<String, Object> configuration = new HashMap<>();
    configuration.put("in", TokenLocation.valueOf(node.get("in").asText().toUpperCase()));
    configuration.put("name", Optional.ofNullable(node.get("name").asText()));

    switch (schemeType) {
      case "apikey":
        return new APIKeySecurityScheme(configuration);
      case "nosec":
        return SecurityScheme.getNoSecurityScheme();
      default:
        throw new IllegalArgumentException("Unknown security scheme type: " + schemeType);
    }
  }
}