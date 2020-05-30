package ch.unisg.ics.interactions.wot.td.affordances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * TODO: add javadoc
 * 
 * @author Andrei Ciortea
 *
 */
public class InteractionAffordance {
  public static final String PROPERTY = "property";
  public static final String EVENT = "event";
  public static final String ACTION = "action";
  
  protected Optional<String> title;
  protected List<String> types;
  protected List<HTTPForm> forms;
  
  protected InteractionAffordance(Optional<String> title, List<String> types, List<HTTPForm> forms) {
    this.title = title;
    this.types = types;
    this.forms = forms;
  }
  
  public Optional<String> getTitle() {
    return title;
  }
  
  public List<String> getTypes() {
    return types;
  }
  
  public List<HTTPForm> getForms() {
    return forms;
  }
  
  /** Abstract builder for interaction affordances. */
  public static abstract class Builder<T extends InteractionAffordance, S extends Builder<T,S>> {
    protected Optional<String> title;
    protected List<String> types;
    protected List<HTTPForm> forms;
    
    protected Builder(List<HTTPForm> forms) {
      this.title = Optional.empty();
      this.types = new ArrayList<String>();
      this.forms = forms;
    }
    
    protected Builder(HTTPForm form) {
      this(new ArrayList<HTTPForm>(Arrays.asList((form))));
    }
    
    @SuppressWarnings("unchecked")
    public S addTitle(String title) {
      this.title = Optional.of(title);
      return (S) this;
    }
    
    @SuppressWarnings("unchecked")
    public S addType(String type) {
      this.types.add(type);
      return (S) this;
    }
    
    @SuppressWarnings("unchecked")
    public S addTypes(List<String> types) {
      this.types.addAll(types);
      return (S) this;
    }
    
    @SuppressWarnings("unchecked")
    public S addForm(HTTPForm form) {
      this.forms.add(form);
      return (S) this;
    }
    
    @SuppressWarnings("unchecked")
    public S addForms(List<HTTPForm> forms) {
      this.forms.addAll(forms);
      return (S) this;
    }
    
    public abstract T build();
  }
}
