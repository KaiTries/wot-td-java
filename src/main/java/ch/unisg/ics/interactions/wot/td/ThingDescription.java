package ch.unisg.ics.interactions.wot.td;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.EventAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.io.InvalidTDException;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.eclipse.rdf4j.model.Model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An immutable representation of a <a href="https://www.w3.org/TR/wot-thing-description/">W3C Web of
 * Things Thing Description (TD)</a>. A <code>ThingDescription</code> is instantiated using a
 * <code>ThingDescription.Builder</code>.
 * <p>
 * The current version does not yet implement all the core vocabulary terms defined by the
 * W3C Recommendation.
 */

@Builder
@Getter
@AllArgsConstructor
public class ThingDescription {
  // @context
  @Builder.Default
  private final Set<String> context = Set.of("https://www.w3.org/2022/wot/td/v1.1");
  // @type
  @Builder.Default
  private final Set<String> type = Set.of();
  // id
  private final String id;
  // title
  private final String title;

  // TODO: Add multiple titles support
  // private final Set<String> titles;

  private final String description;

  // security
  private final Set<String> security;

  // securityDefinitions
  private final Map<String, SecurityScheme> securityDefinitions;

  private final String uri;

  private final String baseURI;

  @Builder.Default
  private final List<PropertyAffordance> properties = List.of();
  @Builder.Default
  private final List<ActionAffordance> actions = List.of();
  @Builder.Default
  private final List<EventAffordance> events = List.of();

  private final Model graph;


  @JsonCreator
  public ThingDescription(
      @JsonProperty("context") Set<String> context,
      @JsonProperty("title") String title,
      @JsonProperty("id") String id,
      @JsonProperty("description") String description,
      @JsonProperty("security") Set<String> security,
      @JsonProperty("securityDefinitions") Map<String, SecurityScheme> securityDefinitions,
      @JsonProperty("uri") String uri,
      @JsonProperty("type") Set<String> types,
      @JsonProperty("baseURI") String baseURI,
      @JsonProperty("properties") List<PropertyAffordance> properties,
      @JsonProperty("actions") List<ActionAffordance> actions,
      @JsonProperty("events") List<EventAffordance> events,
      @JsonProperty("graph") Model graph
  ) {

    if (title == null) {
      throw new InvalidTDException("The title of a Thing cannot be null.");
    }
    this.context = context;
    this.title = title;
    this.id = id;
    this.description = description;

    this.security = security;
    this.securityDefinitions = securityDefinitions;

    // Set up nosec security
    if (this.security.isEmpty()) {
      if (getFirstSecuritySchemeByType(WoTSec.NoSecurityScheme).isPresent()) {
        this.security.add("nosec");
      } else {
        SecurityScheme nosec = SecurityScheme.getNoSecurityScheme();
        this.security.add("nosec");
        this.securityDefinitions.put("nosec", nosec);
      }
    }

    this.uri = uri;
    this.type = types;
    this.baseURI = baseURI;

    this.properties = properties;
    this.actions = actions;
    this.events = events;

    this.graph = graph;
  }

  /**
   * Gets the {@link SecurityScheme} that matches a given security definition name.
   *
   * @param name the name of the security definition
   * @return an <code>Optional</code> with the security scheme (empty if not found)
   */
  public Optional<SecurityScheme> getSecuritySchemeByDefinition(String name) {
    if (securityDefinitions.containsKey(name)) {
      return Optional.of(securityDefinitions.get(name));
    }
    return Optional.empty();
  }

  /**
   * Gets the {@link SecurityScheme} that matches a given security scheme name.
   *
   * @param name the name of the security scheme
   * @return an <code>Optional</code> with the security scheme (empty if not found)
   */
  public Optional<SecurityScheme> getFirstSecuritySchemeByName(String name) {
    for (SecurityScheme securityScheme : securityDefinitions.values()) {

      if (securityScheme.getScheme().equals(name)) {
        return Optional.of(securityScheme);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets the first {@link SecurityScheme} that matches a given semantic type.
   *
   * @param type the semantic type of the security scheme
   * @return an <code>Optional</code> with the security scheme (empty if not found)
   */
  public Optional<SecurityScheme> getFirstSecuritySchemeByType(String type) {
    for (SecurityScheme securityScheme : securityDefinitions.values()) {

      if (securityScheme.getSemanticTypes().contains(type)) {
        return Optional.of(securityScheme);
      }
    }
    return Optional.empty();
  }

  /**
   * Gets a set with all the semantic types of the action affordances provided by the described
   * thing.
   *
   * @return The set of semantic types, can be empty.
   */
  public Set<String> getSupportedActionTypes() {
    Set<String> supportedActionTypes = new HashSet<String>();

    for (ActionAffordance action : actions) {
      supportedActionTypes.addAll(action.getSemanticTypes());
    }

    return supportedActionTypes;
  }

  /**
   * Gets a property affordance by name, which is specified using the <code>td:name</code> data
   * property defined by the TD vocabulary. Names are mandatory in JSON-based representations. If a
   * name is present, it is unique within the scope of a TD.
   *
   * @param name the name of the property affordance
   * @return an <code>Optional</code> with the property affordance (empty if not found)
   */
  public Optional<PropertyAffordance> getPropertyByName(String name) {
    for (PropertyAffordance property : properties) {
      String propertyName = property.getName();
      if (propertyName.equals(name)) {
        return Optional.of(property);
      }
    }

    return Optional.empty();
  }

  /**
   * Gets a list of property affordances that have a {@link ch.unisg.ics.interactions.wot.td.affordances.Form}
   * hypermedia control for the given operation type.
   * <p>
   * The current implementation supports two operation types for properties: <code>td:readProperty</code>
   * and <code>td:writeProperty</code>.
   *
   * @param operationType a string that captures the operation type
   * @return the list of property affordances
   */
  public List<PropertyAffordance> getPropertiesByOperationType(String operationType) {
    return properties.stream().filter(property -> property.hasFormWithOperationType(operationType))
      .collect(Collectors.toList());
  }

  /**
   * Gets the first {@link PropertyAffordance} annotated with a given semantic type.
   *
   * @param propertyType the semantic type, typically an IRI defined in some ontology
   * @return an <code>Optional</code> with the property affordance (empty if not found)
   */
  public Optional<PropertyAffordance> getFirstPropertyBySemanticType(String propertyType) {
    for (PropertyAffordance property : properties) {
      if (property.getSemanticTypes().contains(propertyType)) {
        return Optional.of(property);
      }
    }

    return Optional.empty();
  }

  /**
   * Gets an action affordance by name, which is specified using the <code>td:name</code> data
   * property defined by the TD vocabulary. Names are mandatory in JSON-based representations. If a
   * name is present, it is unique within the scope of a TD.
   *
   * @param name the name of the action affordance
   * @return an <code>Optional</code> with the action affordance (empty if not found)
   */
  public Optional<ActionAffordance> getActionByName(String name) {
    for (ActionAffordance action : actions) {
      String actionName = action.getName();
      if (actionName.equals(name)) {
        return Optional.of(action);
      }
    }

    return Optional.empty();
  }

  /**
   * Gets a list of action affordances that have a {@link ch.unisg.ics.interactions.wot.td.affordances.Form}
   * hypermedia control for the given operation type.
   * <p>
   * There is one operation type available actions: <code>td:invokeAction</code>. The API will be
   * simplified in future iterations.
   *
   * @param operationType a string that captures the operation type
   * @return the list of action affordances
   */
  public List<ActionAffordance> getActionsByOperationType(String operationType) {
    return actions.stream().filter(action -> action.hasFormWithOperationType(operationType))
      .collect(Collectors.toList());
  }

  /**
   * Gets the first {@link ActionAffordance} annotated with a given semantic type.
   *
   * @param actionType the semantic type, typically an IRI defined in some ontology
   * @return an <code>Optional</code> with the action affordance (empty if not found)
   */
  public Optional<ActionAffordance> getFirstActionBySemanticType(String actionType) {
    for (ActionAffordance action : actions) {
      if (action.getSemanticTypes().contains(actionType)) {
        return Optional.of(action);
      }
    }

    return Optional.empty();
  }

  /**
   * Gets an event affordance by name, which is specified using the <code>td:name</code> data
   * property defined by the TD vocabulary. Names are mandatory in JSON-based representations. If a
   * name is present, it is unique within the scope of a TD.
   *
   * @param name the name of the event affordance
   * @return an <code>Optional</code> with the event affordance (empty if not found)
   */
  public Optional<EventAffordance> getEventByName(String name) {
    for (EventAffordance event : events) {
      String eventName = event.getName();
      if (eventName.equals(name)) {
        return Optional.of(event);
      }
    }

    return Optional.empty();
  }

  /**
   * Gets a list of event affordances that have a {@link ch.unisg.ics.interactions.wot.td.affordances.Form}
   * hypermedia control for the given operation type.
   * <p>
   * The current implementation supports two operation types for properties: <code>td:subscribeEvent</code>
   * and <code>td:unsubscribeEvent</code>.
   *
   * @param operationType a string that captures the operation type
   * @return the list of event affordances
   */
  public List<EventAffordance> getEventsByOperationType(String operationType) {
    return events.stream().filter(event -> event.hasFormWithOperationType(operationType))
      .collect(Collectors.toList());
  }

  /**
   * Gets the first {@link EventAffordance} annotated with a given semantic type.
   *
   * @param eventType the semantic type, typically an IRI defined in some ontology
   * @return an <code>Optional</code> with the event affordance (empty if not found)
   */
  public Optional<EventAffordance> getFirstEventBySemanticType(String eventType) {
    for (EventAffordance event : events) {
      if (event.getSemanticTypes().contains(eventType)) {
        return Optional.of(event);
      }
    }

    return Optional.empty();
  }

  /**
   * Supported serialization formats -- currently only RDF serialization formats, namely Turtle and
   * JSON-LD 1.0. The version of JSON-LD currently supported is the one provided by RDF4J.
   */
  public enum TDFormat {
    RDF_TURTLE,
    RDF_JSONLD
  }
}
