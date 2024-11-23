package ch.unisg.ics.interactions.wot.td.security;

import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class APIKeySecurityScheme extends TokenBasedSecurityScheme {

  protected APIKeySecurityScheme(Map<String, Object> configuration, Set<String> semanticTypes,
                                 TokenLocation in, Optional<String> name) {
    super(SecurityScheme.APIKEY, configuration, semanticTypes, in, name);
  }

  public APIKeySecurityScheme(Map<String, Object> configuration) {
    super(SecurityScheme.APIKEY, configuration,Set.of(),
        (TokenLocation) configuration.get(WoTSec.in),
        Optional.of((String) configuration.get(WoTSec.name)));
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper().registerModule(new Jdk8Module()).writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static class Builder extends TokenBasedSecurityScheme.Builder<APIKeySecurityScheme,
    APIKeySecurityScheme.Builder> {

    public Builder() {
      this.name = Optional.empty();
      this.semanticTypes.add(WoTSec.APIKeySecurityScheme);
      this.addTokenLocation(TokenLocation.QUERY);
    }

    @Override
    public APIKeySecurityScheme build() {
      return new APIKeySecurityScheme(configuration, semanticTypes, in, name);
    }
  }
}
