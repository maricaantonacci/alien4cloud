package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "name" })
@FormProperties({ "name", "description", "expression" })
public class OutputDefinition  implements IValue {
  
  protected String name;
  
  protected String description;
  
  protected String expression;

  @Override
  public boolean isDefinition() {
    return true;
  }

}
