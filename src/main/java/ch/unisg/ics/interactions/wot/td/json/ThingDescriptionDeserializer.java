package ch.unisg.ics.interactions.wot.td.json;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThingDescriptionDeserializer extends JsonDeserializer<ThingDescription> {

  private final ContextDeserializer contextDeserializer;
  private final TypeDeserializer typeDeserializer;
  private final SecurityDefinitionsDeserializer securityDefinitionsDeserializer;
  private final PropertiesDeserializer propertiesDeserializer;

  public ThingDescriptionDeserializer(
      final ContextDeserializer contextDeserializer,
      final TypeDeserializer typeDeserializer,
      final SecurityDefinitionsDeserializer securityDefinitionsDeserializer,
      final PropertiesDeserializer propertiesDeserializer) {
    this.contextDeserializer = contextDeserializer;
    this.typeDeserializer = typeDeserializer;
    this.securityDefinitionsDeserializer = securityDefinitionsDeserializer;
    this.propertiesDeserializer = propertiesDeserializer;
  }

  @Override
  public ThingDescription deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    final ObjectMapper mapper = (ObjectMapper) p.getCodec();
    final JsonNode rootNode = mapper.readTree(p);

    // Deserialize @context
    final JsonNode contextNode = rootNode.get("@context");
    final Set<String> context = contextNode != null
        ? contextDeserializer.deserialize(contextNode.traverse(mapper), ctxt)
        : Set.of();

    // Deserialize @type
    final JsonNode typeNode = rootNode.get("@type");
    if (typeNode == null) {
      throw new IOException("TD missing type \"Thing\"");
    }
    final Set<String> types = typeDeserializer.deserialize(typeNode.traverse(mapper), ctxt);
    if (!types.contains(TD.Thing)) {
      throw new IOException("TD missing type \"Thing\"");
    }

    // Deserialize Title
    final var title = getTextNode(rootNode, TD.title);

    // Deserialize id
    final var id = getTextNode(rootNode, "@id");

    // Deserialize description
    final var description = getTextNode(rootNode, TD.description);

    // Deserialize securityDefinitions
    JsonNode securityDefinitionsNode = rootNode.get(TD.definesSecurityScheme);
    final Map<String, SecurityScheme> securityDefinitions = securityDefinitionsNode != null
        ? securityDefinitionsDeserializer.deserialize(securityDefinitionsNode.traverse(mapper), ctxt)
        : Map.of();

    // Deserialize security
    JsonNode securityNode = rootNode.get(TD.hasSecurityConfiguration);
    Set<String> securitySchemes = getSecurity(securityDefinitions, securityNode);

    // Deserialize Base
    String base = getTextNode(rootNode, TD.baseUri);

    // TODO: should be removed? id is correct
    String uri = id; //getTextNode(rootNode, "uri");

    // Deserialize Properties
    JsonNode propertiesNode = rootNode.get(TD.hasPropertyAffordance);
    List<PropertyAffordance> properties = propertiesNode != null
        ? propertiesDeserializer.deserialize(propertiesNode.traverse(mapper), ctxt)
        : List.of();




    final var tdBuilder = ThingDescription.builder();
    tdBuilder.context(context);
    tdBuilder.title(title);
    tdBuilder.type(types);
    tdBuilder.id(id);
    tdBuilder.description(description);
    tdBuilder.securityDefinitions(securityDefinitions);
    tdBuilder.security(securitySchemes);
    tdBuilder.baseURI(base);
    tdBuilder.properties(properties);















    return new ThingDescription(
        context,
        title,
        id,
        description,
        securitySchemes,
        securityDefinitions,
        uri,
        types, // types
        base,
        properties, // properties
        new ArrayList<>(), // actions
        new ArrayList<>(), // events
        null // graph
    );
  }

  private String getTextNode(final JsonNode rootNode, final String name) {
    final JsonNode nameNode = rootNode.get(name);
    if(nameNode.isArray()) {
      final JsonNode nameValueNode = nameNode.get(0).get("@value");
      return nameValueNode == null ? null : nameValueNode.asText();
    }
    return nameNode.asText();
  }

  private Set<String> getSecurity(final Map<String, SecurityScheme> securityDefinitions,
                                  final JsonNode securityNode) {
    Set<String> securitySchemes = new HashSet<>();
    if (securityNode != null && securityNode.isTextual()) {
      String securityKey = securityNode.asText();
      SecurityScheme resolvedScheme = securityDefinitions.get(securityKey);
      if (resolvedScheme != null) {
        securitySchemes.add(securityKey);
      } else {
        throw new IllegalArgumentException("Unknown security key: " + securityKey);
      }
    }

    return securitySchemes;
  }
}