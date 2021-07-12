package ch.unisg.ics.interactions.wot.td.affordances;

import ch.unisg.ics.interactions.wot.td.vocabularies.COV;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import org.apache.commons.collections.map.MultiKeyMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Form {

  private static final HashMap<String, String> URI_SCHEMES;
  private static final MultiKeyMap DEFAULT_METHOD_BINDING = new MultiKeyMap();
  private static final MultiKeyMap DEFAULT_SUBPROTOCOL_BINDING = new MultiKeyMap();

  //To be moved in a ProtocolBinding class
  static {
    URI_SCHEMES = new HashMap<>();
    URI_SCHEMES.put("http:", "HTTP");
    URI_SCHEMES.put("https:", "HTTP");
    URI_SCHEMES.put("coap:", "CoAP");
    URI_SCHEMES.put("coaps:", "CoAP");

    DEFAULT_METHOD_BINDING.put("HTTP", TD.readProperty, "GET");
    DEFAULT_METHOD_BINDING.put("HTTP", TD.writeProperty, "PUT");
    DEFAULT_METHOD_BINDING.put("HTTP", TD.invokeAction, "POST");
    DEFAULT_METHOD_BINDING.put("CoAP", TD.readProperty, "GET");
    DEFAULT_METHOD_BINDING.put("CoAP", TD.writeProperty, "PUT");
    DEFAULT_METHOD_BINDING.put("CoAP", TD.invokeAction, "POST");
    DEFAULT_METHOD_BINDING.put("CoAP", TD.observeProperty, "GET");
    DEFAULT_METHOD_BINDING.put("CoAP", TD.unobserveProperty, "GET");

    DEFAULT_SUBPROTOCOL_BINDING.put("CoAP", TD.observeProperty, COV.observe);
    DEFAULT_SUBPROTOCOL_BINDING.put("CoAP", TD.unobserveProperty, COV.observe);
  }

  private final String target;
  private final String contentType;
  private final Set<String> operationTypes;
  private final Optional<String> subprotocol;
  private Optional<String> methodName;

  private Form(String href, Optional<String> methodName, String mediaType, Set<String> operationTypes,
               Optional<String> subprotocol) {
    this.methodName = methodName;
    this.target = href;
    this.contentType = mediaType;
    this.operationTypes = operationTypes;
    this.subprotocol = subprotocol;
  }

  public Optional<String> getMethodName() {
    return methodName;
  }

  // Package-level access, used for setting affordance-specific default values after instantiation
  void setMethodName(String methodName) {
    this.methodName = Optional.of(methodName);
  }

  public Optional<String> getMethodName(String operationType) {
    if (!operationTypes.contains(operationType)) {
      throw new IllegalArgumentException("Unknown operation type: " + operationType);
    }

    if (methodName.isPresent()) {
      return methodName;
    }

    if (getProtocol().isPresent()) {
      String protocol = getProtocol().get();

      if (DEFAULT_METHOD_BINDING.containsKey(protocol, operationType)) {
        return Optional.of((String) DEFAULT_METHOD_BINDING.get(protocol, operationType));
      }
    }

    return Optional.empty();
  }

  public String getTarget() {
    return target;
  }

  public String getContentType() {
    return contentType;
  }

  public boolean hasOperationType(String type) {
    return operationTypes.contains(type);
  }

  public Set<String> getOperationTypes() {
    return operationTypes;
  }

  public Optional<String> getSubProtocol() {
    return subprotocol;
  }

  public Optional<String> getSubProtocol(String operationType) {
    if (!operationTypes.contains(operationType)) {
      throw new IllegalArgumentException("Unknown operation type: " + operationType);
    }

    if (subprotocol.isPresent()) {
      return subprotocol;
    }

    if (getProtocol().isPresent()) {
      String protocol = getProtocol().get();

      if (DEFAULT_SUBPROTOCOL_BINDING.containsKey(protocol, operationType)) {
        return Optional.of((String) DEFAULT_SUBPROTOCOL_BINDING.get(protocol, operationType));
      }
    }

    return Optional.empty();
  }

  private Optional<String> getProtocol() {
    Optional<String> uriScheme = URI_SCHEMES.keySet()
      .stream()
      .filter(scheme -> getTarget().contains(scheme))
      .findFirst();

    if (uriScheme.isPresent()) {
      return Optional.of(URI_SCHEMES.get(uriScheme.get()));
    }

    return Optional.empty();
  }

  // Package-level access, used for setting affordance-specific default values after instantiation
  void addOperationType(String operationType) {
    this.operationTypes.add(operationType);
  }

  public static class Builder {
    private final String target;
    private final Set<String> operationTypes;
    private Optional<String> methodName;
    private String contentType;
    private Optional<String> subprotocol;

    public Builder(String target) {
      this.target = target;
      this.methodName = Optional.empty();
      this.contentType = "application/json";
      this.operationTypes = new HashSet<String>();
      this.subprotocol = Optional.empty();
    }

    public Builder addOperationType(String operationType) {
      this.operationTypes.add(operationType);
      return this;
    }

    public Builder addOperationTypes(Set<String> operationTypes) {
      this.operationTypes.addAll(operationTypes);
      return this;
    }

    public Builder setMethodName(String methodName) {
      this.methodName = Optional.of(methodName);
      return this;
    }

    public Builder setContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder addSubProtocol(String subprotocol) {
      this.subprotocol = Optional.of(subprotocol);
      return this;
    }

    public Form build() {
      return new Form(this.target, this.methodName, this.contentType, this.operationTypes,
        this.subprotocol);
    }

  }

}
