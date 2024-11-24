package ch.unisg.ics.interactions.wot.td;

import static org.eclipse.rdf4j.model.util.Values.iri;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * This class is an implementation of the RepresentationFactory interface. It provides methods to
 * create representations of platforms, workspaces, artifacts, and bodies. The representations are
 * serialized as Thing Descriptions using the TDGraphWriter class. The class also includes helper
 * methods for serializing Thing Descriptions.
 */
public class RepresentationFactoryTDImplt {
  private static final String ARTIFACT_NAME_PARAM = "artifactName";
  private static final String ARTIFACT = "Artifact";

  private final HttpInterfaceConfigImpl httpConfig;
  private final WebSubConfigImpl notificationConfig;

  private static final String HMAS = "https://purl.org/hmas/";
  private static final String JACAMO = HMAS + "jacamo/";
  private static final String HASH_ARTIFACT = "#artifact";
  private static final String WEBSUB = "websub";

  private static final String GET = "GET";
  private static final String POST = "POST";
  private static final String DELETE = "DELETE";
  private static final String PUT = "PUT";

  public RepresentationFactoryTDImplt(final HttpInterfaceConfigImpl httpConfig,
                                      final WebSubConfigImpl notificationConfig) {
    this.httpConfig = httpConfig;
    this.notificationConfig = notificationConfig;
  }

  private void addHttpSignifiers(final List<ActionAffordance> listOfAffordances,
                                 final String target,
                                 final String type) {
    addAction(listOfAffordances, "get" + type + "Representation", target, GET, "Perceive" + type);
    addAction(listOfAffordances, "update" + type + "Representation", target, PUT, "Update" + type);
    addAction(listOfAffordances, "delete" + type + "Representation", target, DELETE, "Delete" + type);
  }

  private void addAction(List<ActionAffordance> listOfAffordances,
                         final String name,
                         final String target,
                         final String methodName,
                         final String semanticType) {
    addAction(listOfAffordances, name, target, "application/json", methodName,
        semanticType);
  }

  private void addAction(List<ActionAffordance> listOfAffordances,
                         final String name,
                         final String target,
                         final String contentType,
                         final String methodName,
                         final String semanticType) {
    listOfAffordances.add(
        new ActionAffordance.Builder(
            name,
            new Form.Builder(target)
                .setMethodName(methodName)
                .setContentType(contentType)
                .build()
        ).addSemanticType(JACAMO + semanticType)
            .build()
    );
  }

  private void addWebSub(List<ActionAffordance> listOfAffordances, final String actionName) {
    if (notificationConfig.isEnabled()) {
      listOfAffordances.add(websubActions("subscribeTo" + actionName));
      listOfAffordances.add(websubActions("unsubscribeFrom" + actionName));
    }
  }

  private ActionAffordance websubActions(final String actionName) {
    return new ActionAffordance.Builder(
        actionName,
        new Form.Builder(this.notificationConfig.getWebSubHubUri())
            .setMethodName(POST)
            .setContentType("application/json")
            .addSubProtocol(WEBSUB)// could be used for websub
            .build()
    ).addInputSchema(
            new ObjectSchema
                .Builder()
                .addProperty("callbackIri", new StringSchema.Builder().build())
                .addProperty("mode", (new StringSchema.Builder()).build())
                .addProperty("topic", new StringSchema.Builder().build())
                .build()
        ).addSemanticType("https://purl.org/hmas/websub/" + actionName)
        .build();
  }

  private Model getResourceProfileGraph(final String thingIRI,
                                     final String tdIRI) {
    final Model graph = new LinkedHashModel();

    graph.add(
        iri(thingIRI),
        RDF.TYPE,
        iri(HMAS + "ResourceProfile")
    );

    graph.add(
        iri(thingIRI),
        iri(HMAS + "isProfileOf"),
        iri(tdIRI)
    );
    return graph;
  }

  public ThingDescription createPlatform() {
    final var thingIri = this.httpConfig.getBaseUriTrailingSlash();
    final var td = ThingDescription.builder();
    td.title("Yggdrasil Node");
    td.id(thingIri);
    td.baseURI(thingIri);
    td.type(Set.of(TD.Thing, HMAS + "HypermediaMASPlatform"));

    final List<ActionAffordance> listOfActions = new ArrayList<>();


    addAction(listOfActions, "createWorkspace", this.httpConfig.getWorkspacesUriTrailingSlash(),
        "text/turtle", POST, "createWorkspace");

    addAction(listOfActions, "sparqlGetQuery", thingIri
        + "query/", "application/sparql-query", GET, "sparqlGetQuery");
    addAction(listOfActions, "sparqlPostQuery", thingIri
            + "query/", "application/sparql-query",
        POST, "sparqlPostQuery");

    addWebSub(listOfActions, "Platform");

    if (notificationConfig.isEnabled()) {
      listOfActions.add(
          new ActionAffordance.Builder(
              "subscribeToWorkspaces",
              new Form.Builder(this.httpConfig.getWorkspacesUriTrailingSlash())
                  .setMethodName(GET)
                  .setContentType("application/json")
                  .addSubProtocol(WEBSUB)
                  .build()
          ).build()
      );
    }


    td.actions(listOfActions);

    final var resourceGraph = getResourceProfileGraph(thingIri, thingIri + "#platform");

    td.graph(resourceGraph);
    return td.build();
  }

  public String createPlatformRepresentation() {
    return serializeThingDescription(
        createPlatform()
    );
  }

  public ThingDescription createWorkspace(
      final String workspaceName,
      final Set<String> artifactTemplates,
      final boolean isCartagoWorkspace
  ) {
    final var thingUri = this.httpConfig.getWorkspaceUri(workspaceName);
    final var td = ThingDescription.builder();
    td.title(workspaceName);
    td.id(thingUri);
    td.baseURI(thingUri);
    td.type(Set.of(TD.Thing, HMAS + "Workspace"));

    final List<ActionAffordance> l = new ArrayList<>();

    addAction(l, "createSubWorkspace", thingUri,
        "text/turtle", POST, "createSubWorkspace");

    addHttpSignifiers(l, thingUri, "Workspace");

    addAction(l, "createArtifact", this.httpConfig.getArtifactsUriTrailingSlash(workspaceName),
        "text/turtle", POST, "createArtifact");


    if (isCartagoWorkspace) {
      addAction(l, "joinWorkspace", thingUri + "/join", POST, "JoinWorkspace");
      addAction(l, "quitWorkspace", thingUri + "/leave", POST, "QuitWorkspace");
      l.add(
          new ActionAffordance.Builder(
              "makeArtifact",
              new Form.Builder(this.httpConfig.getArtifactsUriTrailingSlash(workspaceName))
                  .build()
          )
              .addInputSchema(
                  new ObjectSchema
                      .Builder()
                      .addProperty(
                          "artifactClass",
                          new StringSchema.Builder().addEnum(artifactTemplates)
                              .addSemanticType(JACAMO + "ArtifactTemplate")
                              .build()
                      )
                      .addProperty(ARTIFACT_NAME_PARAM,
                          new StringSchema.Builder().addSemanticType(JACAMO + "ArtifactName")
                              .build())
                      .addProperty("initParams",
                          new ArraySchema.Builder()
                              .addSemanticType(JACAMO + "InitParams").build())
                      .addRequiredProperties("artifactClass", ARTIFACT_NAME_PARAM)
                      .build()
              ).addSemanticType(JACAMO + "MakeArtifact")
              .build()
      );

    }

    if (notificationConfig.isEnabled()) {
      l.add(
          new ActionAffordance.Builder(
              "getSubWorkspaces",
              new Form.Builder(this.httpConfig.getWorkspacesUri() + "?parent=" + workspaceName)
                  .setMethodName(GET)
                  .addSubProtocol(WEBSUB)
                  .build()
          ).build()
      );
    }

    addWebSub(l, "Workspace");
    final var resourceGraph = getResourceProfileGraph(thingUri, thingUri + "#workspace");

    td.actions(l);
    td.graph(resourceGraph);

    return td.build();
  }

  public String createWorkspaceRepresentation(
      final String workspaceName,
      final Set<String> artifactTemplates,
      final boolean isCartagoWorkspace
  ) {
    return serializeThingDescription(
        createWorkspace(workspaceName, artifactTemplates, isCartagoWorkspace)
    );
  }

  public ThingDescription createArtifact(final String workspaceName, final String artifactName,
                                             final String semanticType,
                                             final boolean isCartagoArtifact) {
    return createArtifact(
        workspaceName,
        artifactName,
        SecurityScheme.getNoSecurityScheme(),
        semanticType,
        new LinkedHashModel(),
        Multimaps.newListMultimap(new HashMap<>(), ArrayList::new),
        isCartagoArtifact
    );
  }

  public ThingDescription createArtifact(final String workspaceName, final String artifactName,
                                             final String semanticType, final Model metadata,
                                             final ListMultimap<String, Object> actionAffordances,
                                             final boolean isCartagoArtifact) {
    return createArtifact(
        workspaceName,
        artifactName,
        SecurityScheme.getNoSecurityScheme(),
        semanticType,
        metadata,
        actionAffordances,
        isCartagoArtifact
    );
  }


  public ThingDescription createArtifact(
      final String workspaceName,
      final String artifactName,
      final SecurityScheme securityScheme,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, Object> actionAffordances,
      final boolean isCartagoArtifact
  ) {
    final ListMultimap<String, ActionAffordance> actionAffordancesMap =
        Multimaps.newListMultimap(new HashMap<>(),
            ArrayList::new);
    actionAffordances.entries().forEach(entry -> {
      final var actionName = entry.getKey();
      final var action = (ActionAffordance) entry.getValue();
      actionAffordancesMap.put(actionName, action);
    });


    final var thingUri = this.httpConfig.getArtifactUri(workspaceName, artifactName);

    final var td = ThingDescription.builder();
    td.title(artifactName);
    td.id(thingUri);
    td.type(Set.of(TD.Thing, HMAS + ARTIFACT));
    td.baseURI(thingUri);

    final List<ActionAffordance> l = (List<ActionAffordance>) actionAffordancesMap.values();

    addHttpSignifiers(l, thingUri, ARTIFACT);

    if (isCartagoArtifact) {
      l.add(
          new ActionAffordance.Builder(
              "focusArtifact",
              new Form.Builder(this.httpConfig.getWorkspaceUriTrailingSlash(workspaceName)
                  + "focus")
                  .setMethodName(POST)
                  .build()
          )
              .addInputSchema(
                  new ObjectSchema
                      .Builder()
                      .addProperty(ARTIFACT_NAME_PARAM,
                          new StringSchema.Builder().addEnum(Collections.singleton(artifactName))
                              .build())
                      .addProperty("callbackIri", new StringSchema.Builder().build())
                      .addRequiredProperties(ARTIFACT_NAME_PARAM, "callbackIri")
                      .build()
              ).addSemanticType(JACAMO + "Focus")
              .build()
      );
    }

    addWebSub(l, ARTIFACT);
    final var resourceGraph = getResourceProfileGraph(thingUri, thingUri + HASH_ARTIFACT);

    td.actions(l);
    td.graph(resourceGraph);

    return td.build();

  }

  public String createArtifactRepresentation(
      final String workspaceName,
      final String artifactName,
      final SecurityScheme securityScheme,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, Object> actionAffordances,
      final boolean isCartagoArtifact
  ) {
    return serializeThingDescription(
        createArtifact(
            workspaceName,
            artifactName,
            securityScheme,
            semanticType,
            metadata,
            actionAffordances,
            isCartagoArtifact
        )
    );
  }

  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final Model metadata) {
    return createBodyRepresentation(workspaceName, agentName, SecurityScheme.getNoSecurityScheme(),
        metadata);
  }

  public ThingDescription createBody(
      final String workspaceName,
      final String agentName,
      final SecurityScheme securityScheme,
      final Model metadata
  ) {
    final var bodyUri = this.httpConfig.getAgentBodyUri(workspaceName, agentName);
    final var td = ThingDescription.builder();
    td.title(agentName);
    td.id(bodyUri);
    td.baseURI(bodyUri);
    td.type(Set.of(TD.Thing, HMAS + ARTIFACT, JACAMO + "Body"));

    final List<ActionAffordance> l = new ArrayList<>();
    addWebSub(l, "Agent");

    td.actions(l);
    final var rG = getResourceProfileGraph(bodyUri, bodyUri + HASH_ARTIFACT);

    td.graph(rG);

    return td.build();
  }

  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final SecurityScheme securityScheme,
      final Model metadata
  ) {
    return serializeThingDescription(
        createBody(workspaceName, agentName, securityScheme, metadata)
    );
  }

  private String serializeThingDescription(final ThingDescription td) {
    return new TDGraphWriter(td)
        .setNamespace("td", "https://www.w3.org/2019/wot/td#")
        .setNamespace("htv", "http://www.w3.org/2011/http#")
        .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
        .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
        .setNamespace("dct", "http://purl.org/dc/terms/")
        .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
        .setNamespace("hmas", HMAS)
        .setNamespace("ex", "http://example.org/")
        .setNamespace("jacamo", JACAMO)
        .setNamespace(WEBSUB, HMAS + "websub/")
        .write();
  }
}
