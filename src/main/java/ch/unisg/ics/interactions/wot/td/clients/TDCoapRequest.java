package ch.unisg.ics.interactions.wot.td.clients;

import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.vocabularies.COV;
import com.google.gson.Gson;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper for constructing and executing a CoAP request based on a given <code>ThingDescription</code>.
 * When constructing the request, clients can set payloads that conform to a <code>DataSchema</code>.
 */
public class TDCoapRequest {
  private final static Logger LOGGER = Logger.getLogger(TDHttpRequest.class.getCanonicalName());

  private final Form form;
  private final Request request;

  public TDCoapRequest(Form form, String operationType) {
    this.form = form;

    Optional<String> methodName = form.getMethodName(operationType);

    if (methodName.isPresent()) {
      this.request = new Request(CoAP.Code.valueOf(methodName.get()));
      this.request.setURI(form.getTarget());
    } else {
      throw new IllegalArgumentException("No default binding for the given operation type: "
        + operationType);
    }

    if (form.getSubProtocol().isPresent() && form.getSubProtocol().get().equals(COV.observe)) {
      this.request.setObserve();
    }

    this.request.getOptions().setContentFormat(MediaTypeRegistry.parse(form.getContentType()));
  }

  /**
   * Executes a synchronous CoAP request.
   *
   * @return the CoAP response to the request
   * @throws InterruptedException if any issue occurred
   */
  public TDCoapResponse executeSync() throws InterruptedException {
    Request coapRequest = request.send();
    return new TDCoapResponse(coapRequest.waitForResponse());
  }

  /**
   * Executes an asynchronous CoAP request and invokes the specified
   * observer {@link TDCoapObserver} each time a notification arrives.
   *
   * @throws RuntimeException if the list of observers for the request is empty
   */
  public void executeAsync() {
    if (request.getMessageObservers().isEmpty()) {
      throw new RuntimeException("No observer found for the asynchronous request.");
    }
    request.send();
  }

  /**
   * Adds the specified observer. If an asynchronous request is executed with
   * {@link #executeAsync()}, the observer is invoked each time a notification
   * arrives.
   *
   * @param observer the observer
   */
  public void addObserver(TDCoapObserver observer) {
    request.addMessageObserver(observer.getMessageObserverAdapter());
  }

  /**
   * Removes the specified observer.
   *
   * @param observer the observer
   */
  public void removeObserver(TDCoapObserver observer) {
    request.removeMessageObserver(observer.getMessageObserverAdapter());
  }

  public TDCoapRequest addOption(String key, String value) {
    // TODO Support CoAP options e.g. for observation flag
    return null;
  }

  public TDCoapRequest setPrimitivePayload(DataSchema dataSchema, boolean value)
    throws IllegalArgumentException {
    if (dataSchema.getDatatype().equals(DataSchema.BOOLEAN)) {
      request.setPayload(String.valueOf(value));
    } else {
      throw new IllegalArgumentException("The payload's datatype does not match BooleanSchema "
        + "(payload datatype: " + dataSchema.getDatatype() + ")");
    }

    return this;
  }

  public TDCoapRequest setPrimitivePayload(DataSchema dataSchema, String value)
    throws IllegalArgumentException {
    if (dataSchema.getDatatype().equals(DataSchema.STRING)) {
      request.setPayload(String.valueOf(value));
    } else {
      throw new IllegalArgumentException("The payload's datatype does not match StringSchema "
        + "(payload datatype: " + dataSchema.getDatatype() + ")");
    }

    return this;
  }

  public TDCoapRequest setPrimitivePayload(DataSchema dataSchema, long value)
    throws IllegalArgumentException {
    if (dataSchema.getDatatype().equals(DataSchema.INTEGER)
      || dataSchema.getDatatype().equals(DataSchema.NUMBER)) {
      request.setPayload(String.valueOf(value));
    } else {
      throw new IllegalArgumentException("The payload's datatype does not match IntegerSchema or "
        + "NumberSchema (payload datatype: " + dataSchema.getDatatype() + ")");
    }

    return this;
  }

  public TDCoapRequest setPrimitivePayload(DataSchema dataSchema, double value)
    throws IllegalArgumentException {
    if (dataSchema.getDatatype().equals(DataSchema.NUMBER)) {
      request.setPayload(String.valueOf(value));
    } else {
      throw new IllegalArgumentException("The payload's datatype does not match NumberSchema "
        + "(payload datatype: " + dataSchema.getDatatype() + ")");
    }

    return this;
  }

  /**
   * Sets a payload of type <code>ObjectSchema</code>. The object payload is given as a map where:
   * <ul>
   * <li>a key is a string that represents either a semantic type or an object property name</li>
   * <li>a value can be a primitive, an object represented as a <code>Map&lt;String,Object&gt;</code>
   * (that is, a nested object), or an ordered list of values of type <code>List&lt;Object&gt;</code></li>
   * </ul>
   *
   * @param objectSchema schema to be used for validating the payload and constructing the body of
   *                     the request
   * @param payload      the actual payload
   * @return this <code>TDCoapRequest</code>
   */
  public TDCoapRequest setObjectPayload(ObjectSchema objectSchema, Map<String, Object> payload) {
    if (objectSchema.validate(payload)) {
      Map<String, Object> instance = objectSchema.instantiate(payload);
      String body = new Gson().toJson(instance);
      request.setPayload(body);
    }

    return this;
  }

  /**
   * Sets a payload of type <code>ArraySchema</code>. The payload is given as an ordered list of
   * values of type <code>List&lt;Object&gt;</code>. Values can be primitives, objects represented
   * as <code>Map&lt;String,Object&gt;</code>, or lists of values (that is, nested lists).
   *
   * @param arraySchema schema used for validating the payload and constructing the body of
   *                    the request
   * @param payload     the actual payload
   * @return this <code>TDCoapRequest</code>
   */
  public TDCoapRequest setArrayPayload(ArraySchema arraySchema, List<Object> payload) {
    if (arraySchema.validate(payload)) {
      String body = new Gson().toJson(payload);
      request.setPayload(body);
    }

    return this;
  }

  public String getPayloadAsString() {
    return request.getPayloadString();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[TDCoapRequest] Method: " + request.getCode().name());

    try {
      builder.append(", Target: " + request.getURI());


      builder.append(", " + request.getOptions().toString());


      if (request.getPayload() != null) {
        builder.append(", Payload: " + request.getPayloadString());
      }
    } catch (UnsupportedOperationException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
    }

    return builder.toString();
  }

  Request getRequest() {
    return this.request;
  }

}
