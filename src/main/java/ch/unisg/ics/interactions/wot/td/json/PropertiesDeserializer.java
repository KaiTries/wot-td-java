package ch.unisg.ics.interactions.wot.td.json;

import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.vocabularies.JSONSchema;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.jsonldjava.utils.Obj;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesDeserializer extends JsonDeserializer<List<PropertyAffordance>> {

  @Override
  public List<PropertyAffordance> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    JsonNode propertiesNode = p.getCodec().readTree(p);
    List<PropertyAffordance> propertiesList = new ArrayList<>();

    System.out.println(propertiesNode);

    propertiesNode.fields().forEachRemaining(entry -> {
      JsonNode propertyJson = entry.getValue();

      String propertyName = entry.getKey();

      final var title = getTextNode(propertyJson, "title");
      final var description = getTextNode(propertyJson, "description");
      final var typeRaw = getTextNode(propertyJson, "type");
      final var type = getType(typeRaw);


      final var formsJson = propertyJson.get("forms");
      final List<Form> forms;
      try {
        forms = getForms(formsJson);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }


      final var propertyAffordance = new PropertyAffordance.Builder(propertyName)
          .addTitle(title)
          .addDescription(description)
          .addSemanticType(type)
          .addForms(forms)
          .build();

      propertiesList.add(propertyAffordance);
    });


    return propertiesList;
  }

  private String getTextNode(final JsonNode rootNode, final String name) {
    final JsonNode nameNode = rootNode.get(name);
    return nameNode == null ? null : nameNode.asText();
  }

  private String getType(final String type) {
    return switch (type) {
      case "object" -> JSONSchema.ObjectSchema;
      case "array" -> JSONSchema.ArraySchema;
      case "boolean" -> JSONSchema.BooleanSchema;
      case "string" -> JSONSchema.StringSchema;
      case "number" -> JSONSchema.NumberSchema;
      case "integer" -> JSONSchema.IntegerSchema;
      case null -> JSONSchema.NullSchema;
      default -> throw new IllegalArgumentException("Couldnt parse schema type");
    };
  }

  private List<Form> getForms(JsonNode formsJson) throws JsonProcessingException {
    List<Form> forms = new ArrayList<>();
    final var o = new ObjectMapper().registerModule(new Jdk8Module());

    if (formsJson.isArray()) {
      for (JsonNode formNode : formsJson) {

        Form f = o.treeToValue(formNode, Form.class);

        forms.add(f);
      }

    } else {
      throw new IllegalArgumentException("Forms must be specified in an array");
    }

    return forms;
  }
}
