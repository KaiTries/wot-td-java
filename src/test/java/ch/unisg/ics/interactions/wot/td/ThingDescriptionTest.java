package ch.unisg.ics.interactions.wot.td;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.EventAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.io.ReadWriteUtils;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.json.ContextDeserializer;
import ch.unisg.ics.interactions.wot.td.json.PropertiesDeserializer;
import ch.unisg.ics.interactions.wot.td.json.SecurityDefinitionsDeserializer;
import ch.unisg.ics.interactions.wot.td.json.ThingDescriptionDeserializer;
import ch.unisg.ics.interactions.wot.td.json.TypeDeserializer;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.TokenBasedSecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.HCTL;
import ch.unisg.ics.interactions.wot.td.vocabularies.HTV;
import ch.unisg.ics.interactions.wot.td.vocabularies.JSONSchema;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.jsonld.JSONLDParser;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class ThingDescriptionTest {

  private ThingDescription commonTd;

  @Before
  public void init() {
    PropertyAffordance prop0 = new PropertyAffordance.Builder("temp",
      new Form.Builder("http://example.org/prop0")
        .addOperationType(TD.readProperty)
        .build())
      .addSemanticType("ex:Temp")
      .addSemanticType("ex:Value")
      .build();

    PropertyAffordance prop1 = new PropertyAffordance.Builder("air-quality",
      new Form.Builder("http://example.org/prop1")
        .addOperationType(TD.readProperty)
        .build())
      .addSemanticType("ex:AirQuality")
      .addSemanticType("ex:Value")
      .build();

    ActionAffordance action0 = new ActionAffordance.Builder("setTemp",
      new Form.Builder("http://example.org/action0")
        .addOperationType(TD.invokeAction)
        .build())
      .addSemanticType("ex:SetTemp")
      .addSemanticType("ex:ModifyEnv")
      .build();

    ActionAffordance action1 = new ActionAffordance.Builder("openVentilator",
      new Form.Builder("http://example.org/action1")
        .addOperationType(TD.invokeAction)
        .build())
      .addSemanticType("ex:OpenVentilator")
      .addSemanticType("ex:ModifyEnv")
      .build();

    EventAffordance event0 = new EventAffordance.Builder("overheating",
      new Form.Builder("http://example.org/event0")
        .addOperationType(TD.subscribeEvent)
        .build())
      .addSemanticType("ex:Overheating")
      .addSemanticType("ex:Alarm")
      .build();

    EventAffordance event1 = new EventAffordance.Builder("smoke-alarm",
      new Form.Builder("http://example.org/event1")
        .addOperationType(TD.subscribeEvent)
        .build())
      .addSemanticType("ex:SmokeAlarm")
      .addSemanticType("ex:Alarm")
      .build();


    final Map<String, Object> c = Map.of(
        WoTSec.in, TokenBasedSecurityScheme.TokenLocation.HEADER,
        WoTSec.name, "some_name"
    );
    final var apiSc = new APIKeySecurityScheme(c);

    commonTd = ThingDescription.builder()
        .type(Set.of(TD.Thing))
        .title("A Thing")
        .security(Set.of("api_sc"))
        .securityDefinitions(Map.of("api_sc", apiSc))
        .properties(List.of(prop0,prop1))
        .actions(List.of(action0,action1))
        .events(List.of(event0,event1))
        .build();
  }

  @Test
  public void testOutputJson() throws JsonProcessingException {
    final var o = new ObjectMapper();
    o.registerModule(new Jdk8Module());
    o.enable(SerializationFeature.INDENT_OUTPUT);
    o.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    String serialized = o.writeValueAsString(commonTd);

   System.out.println(serialized);
    var tdWriter = new TDGraphWriter(commonTd);
    tdWriter.setNamespace("td", TD.PREFIX);
    tdWriter.setNamespace("wotsec", WoTSec.PREFIX);
    var s = tdWriter.write();
    //System.out.println(s);
    var t = TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, s);
    var tdWriter2 = new TDGraphWriter(t);
    tdWriter2.setNamespace("td", TD.PREFIX);
    tdWriter2.setNamespace("wotsec", WoTSec.PREFIX);
    var s2 = tdWriter2.write();
    // System.out.println(s2);
  }


  @Test
  public void testInputJson() throws IOException, URISyntaxException, JsonLdError {
    final var inputJsonLdRdfString = Files.readString(
        Path.of(ClassLoader.getSystemResource("rdfJsonLDoutput.jsonld").toURI()),
        StandardCharsets.UTF_8
    );
    final var inputJsonLdString = Files.readString(
        Path.of(ClassLoader.getSystemResource("input.td.jsonld").toURI()),
        StandardCharsets.UTF_8
    );

    var j = JsonLd.toRdf("file:/Users/kaischultz/github/wot-td-java/src/test/resources" +
        "/input.td.jsonld").get();

    //var t = JsonLd.frame("file:/Users/kaischultz/github/wot-td-java/src/test/resources" +
    //    "/input.td.jsonld","");
    /*
    JSONLDParser parser = new JSONLDParser();
    parser.set(JSONLDSettings.SECURE_MODE, false);
    InputStream inputStream = new ByteArrayInputStream(inputJsonLdString.getBytes(StandardCharsets.UTF_8));
    final var model = new LinkedHashModel();
    model.setNamespace("td", TD.PREFIX);
    model.setNamespace("wotsec", WoTSec.PREFIX);
    model.setNamespace("htv", HTV.PREFIX);
    model.setNamespace("hctl",HCTL.PREFIX);
    model.setNamespace("schema",JSONSchema.PREFIX);
    model.setNamespace("xml","http://www.w3.org/2001/XMLSchema#");
    model.setNamespace("hmas", "https://purl.org/hmas/");
    parser.setRDFHandler(new StatementCollector(model));
    parser.parse(inputStream);

    final var please = ReadWriteUtils.modelToString(model, RDFFormat.TURTLE, null);






    // System.out.println(please);
    /*
    StringWriter w = new StringWriter();

    JSONLDWriter writer = (JSONLDWriter) new JSONLDWriterFactory().getWriter(w);

    final var settings = writer.getSupportedSettings();

    writer.set(JSONLDSettings.OPTIMIZE, true);

    System.out.println(settings);


    writer.startRDF();
    model.forEach(writer::consumeStatement);
    writer.endRDF();

    System.out.println(w.toString());
    */


    // var tJ = JsonUtil.prettyPrint(j);


    ObjectMapper o = new ObjectMapper();


    o.registerModule(new Jdk8Module());
    SimpleModule module = new SimpleModule();
    module.addDeserializer(ThingDescription.class, new ThingDescriptionDeserializer(
                new ContextDeserializer(),
                new TypeDeserializer(),
                new SecurityDefinitionsDeserializer(),
                new PropertiesDeserializer()
            )
        );
    o.registerModule(module);

    ThingDescription t = o.readValue(inputJsonLdString, ThingDescription.class);


    var s = new TDGraphWriter(t);
    s.setNamespace("td",TD.PREFIX);
    s.setNamespace("wotsec", WoTSec.PREFIX);
    s.setNamespace("jsonSchema", JSONSchema.PREFIX);
    s.setNamespace("htv", HTV.PREFIX);
    s.setNamespace("hctl", HCTL.PREFIX);
    System.out.println(s.write());

    o.registerModule(module);
    o.enable(SerializationFeature.INDENT_OUTPUT);
    o.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    String serialized = o.writeValueAsString(t);

    // System.out.println(serialized);







  }

  @Test
  public void testPlatform() throws JsonProcessingException {
    HttpInterfaceConfigImpl httpInterfaceConfig = new HttpInterfaceConfigImpl(
        "localhost",
        8080,
        "localhost:8080"
    );
    WebSubConfigImpl webSubConfig = new WebSubConfigImpl(
      false,
        "localhost:8080/hub"
    );

    RepresentationFactoryTDImplt factoryTDImplt = new RepresentationFactoryTDImplt(
        httpInterfaceConfig,
        webSubConfig
    );

    final ThingDescription platform = factoryTDImplt.createPlatform();
    final var s = ReadWriteUtils.tdToJsonLD(platform);
    System.out.println(s);

  }

  @Test
  public void testTitle() {
    ThingDescription td = ThingDescription.builder().title("My Thing").build();

    assertEquals("My Thing", td.getTitle());
  }

  @Test(expected = NullPointerException.class)
  public void testTitleNull() {
    ThingDescription.builder().title(null).build();
  }

  @Test
  public void testURI() {
    ThingDescription td = ThingDescription.builder()
        .title("My Thing")
        .id("http://example.org/#thing")
        .build();

    assertEquals("http://example.org/#thing", td.getId());
  }

  @Test
  public void testOneType() {
    ThingDescription td = ThingDescription.builder()
        .title("My Thing")
        .type(Set.of("http://w3id.org/eve#Artifact"))
        .build();

    assertEquals(1, td.getType().size());
    assertTrue(td.getType().contains("http://w3id.org/eve#Artifact"));
  }

  @Test
  public void testMultipleTypes() {
    ThingDescription td = ThingDescription.builder()
        .title("My Thing")
        .type(Set.of(TD.Thing, "http://w3id.org/eve#Artifact", "http://iot-schema.org/eve#Light"))
        .build();

    assertEquals(3, td.getType().size());
    assertTrue(td.getType().contains(TD.Thing));
    assertTrue(td.getType().contains("http://w3id.org/eve#Artifact"));
    assertTrue(td.getType().contains("http://iot-schema.org/eve#Light"));
  }

  @Test
  public void testBaseURI() {
    ThingDescription td = ThingDescription.builder()
        .title("My Thing")
        .id("http://example.org/#thing")
        .baseURI("http://example.org/")
        .build();

    assertEquals("http://example.org/", td.getBaseURI());
  }

  @Test
  public void testGetFirstSecuritySchemeByType() {
    ThingDescription td = ThingDescription.builder()
        .title("Secured Thing")
        .securityDefinitions(Map.of("nosec_sc", SecurityScheme.getNoSecurityScheme(),
            "apikey_sc", new APIKeySecurityScheme.Builder().build()))
        .build();

    Optional<SecurityScheme> scheme = td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme);
    assertTrue(scheme.isPresent());
    assertTrue(SecurityScheme.APIKEY.equals(scheme.get().getScheme()));
  }

  @Test
  public void testGetPropertyByName() {
    Optional<PropertyAffordance> prop0 = commonTd.getPropertyByName("temp");
    assertTrue(prop0.isPresent());
    assertTrue(prop0.get().getSemanticTypes().contains("ex:Temp"));

    Optional<PropertyAffordance> prop1 = commonTd.getPropertyByName("air-quality");
    assertTrue(prop1.isPresent());
    assertTrue(prop1.get().getSemanticTypes().contains("ex:AirQuality"));
  }

  @Test
  public void testGetPropertiesByOperationType() {
    List<PropertyAffordance> readProps = commonTd.getPropertiesByOperationType(TD.readProperty);
    assertEquals(2, readProps.size());

    List<EventAffordance> writeProps = commonTd.getEventsByOperationType(TD.writeProperty);
    assertEquals(0, writeProps.size());
  }

  @Test
  public void testGetFirstPropertyBySemanticType() {
    Optional<PropertyAffordance> existingProp = commonTd.getFirstPropertyBySemanticType("ex:Value");
    assertTrue(existingProp.isPresent());
    assertTrue(existingProp.get().getSemanticTypes().contains("ex:Value"));

    Optional<PropertyAffordance> unknownProp = commonTd.getFirstPropertyBySemanticType("ex:NoValue");
    assertFalse(unknownProp.isPresent());
  }

  @Test
  public void testGetActionByName() {
    Optional<ActionAffordance> action0 = commonTd.getActionByName("setTemp");
    assertTrue(action0.isPresent());
    assertTrue(action0.get().getSemanticTypes().contains("ex:SetTemp"));

    Optional<ActionAffordance> action1 = commonTd.getActionByName("openVentilator");
    assertTrue(action1.isPresent());
    assertTrue(action1.get().getSemanticTypes().contains("ex:OpenVentilator"));
  }

  @Test
  public void testGetActionsByOperationType() {
    List<ActionAffordance> invokeActions = commonTd.getActionsByOperationType(TD.invokeAction);
    assertEquals(2, invokeActions.size());

    List<ActionAffordance> writeProps = commonTd.getActionsByOperationType(TD.writeProperty);
    assertEquals(0, writeProps.size());
  }

  @Test
  public void testGetFirstActionBySemanticType() {
    Optional<ActionAffordance> existingAction = commonTd.getFirstActionBySemanticType("ex:ModifyEnv");
    assertTrue(existingAction.isPresent());
    assertTrue(existingAction.get().getSemanticTypes().contains("ex:ModifyEnv"));

    Optional<ActionAffordance> unknownAction = commonTd.getFirstActionBySemanticType("ex:NoModifyEnv");
    assertFalse(unknownAction.isPresent());
  }

  @Test
  public void testGetEventByName() {
    Optional<EventAffordance> event0 = commonTd.getEventByName("overheating");
    assertTrue(event0.isPresent());
    assertTrue(event0.get().getSemanticTypes().contains("ex:Overheating"));

    Optional<EventAffordance> event1 = commonTd.getEventByName("smoke-alarm");
    assertTrue(event1.isPresent());
    assertTrue(event1.get().getSemanticTypes().contains("ex:SmokeAlarm"));

    Optional<EventAffordance> event2 = commonTd.getEventByName("unknown-event");
    assertFalse(event2.isPresent());
  }

  @Test
  public void testGetEventsByOperationType() {
    List<EventAffordance> subscribeEvents = commonTd.getEventsByOperationType(TD.subscribeEvent);
    assertEquals(2, subscribeEvents.size());

    List<EventAffordance> unsubscribeEvents = commonTd.getEventsByOperationType(TD.unsubscribeEvent);
    assertEquals(0, unsubscribeEvents.size());
  }

  @Test
  public void testGetFirstEventBySemanticType() {
    Optional<EventAffordance> existingEvent = commonTd.getFirstEventBySemanticType("ex:Alarm");
    assertTrue(existingEvent.isPresent());
    assertTrue(existingEvent.get().getSemanticTypes().contains("ex:Alarm"));

    Optional<EventAffordance> unknownEvent = commonTd.getFirstEventBySemanticType("ex:NoAlarm");
    assertFalse(unknownEvent.isPresent());
  }
}
