package ch.unisg.ics.interactions.wot.td;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.StringReader;

public class ModelDeserializer extends JsonDeserializer<Model> {

  @Override
  public Model deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = p.getCodec().readTree(p);
    String rdfContent = node.toString();
    return Rio.parse(new StringReader(rdfContent), "", RDFFormat.JSONLD);
  }
}