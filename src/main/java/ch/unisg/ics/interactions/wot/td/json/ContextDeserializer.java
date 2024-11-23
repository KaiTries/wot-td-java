package ch.unisg.ics.interactions.wot.td.json;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ContextDeserializer extends JsonDeserializer<Set<String>> {

  @Override
  public Set<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.getCodec().readTree(p);
    Set<String> contextSet = new HashSet<>();

    if (node.isTextual()) {
      // Single string case
      contextSet.add(node.asText());
    } else if (node.isArray()) {
      // Array of strings case
      for (JsonNode element : node) {
        if (element.isTextual()) {
          contextSet.add(element.asText());
        } else {
          throw new IllegalArgumentException("Array elements of @context must be strings");
        }
      }
    } else {
      throw new IllegalArgumentException("@context must be a string or an array of strings");
    }

    return contextSet;
  }
}