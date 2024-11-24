package ch.unisg.ics.interactions.wot.td.json;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class JsonUtil {


  public static String prettyPrint(JsonValue jsonValue) {
    final var sw = new StringWriter();

    try {

      Map<String, Object> properties = new HashMap<>(1);
      properties.put(JsonGenerator.PRETTY_PRINTING, true);


      JsonGeneratorFactory jf = Json.createGeneratorFactory(properties);
      JsonGenerator jg = jf.createGenerator(sw);

      jg.write(jsonValue).close();


    } catch (Exception e) {
    }


    return sw.toString();
  }
}
