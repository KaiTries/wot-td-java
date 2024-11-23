package ch.unisg.ics.interactions.wot.td.json;

import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class SecuritySchemeSerializer extends JsonSerializer<SecurityScheme> {

  @Override
  public void serialize(SecurityScheme securityScheme, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("scheme", securityScheme.getScheme());
    gen.writeFieldName("semanticTypes");
    gen.writeObject(securityScheme.getSemanticTypes());

    // Unpack the configuration map
    for (Map.Entry<String, Object> entry : securityScheme.getConfiguration().entrySet()) {
      gen.writeObjectField(entry.getKey(), entry.getValue());
    }

    gen.writeEndObject();
  }
}