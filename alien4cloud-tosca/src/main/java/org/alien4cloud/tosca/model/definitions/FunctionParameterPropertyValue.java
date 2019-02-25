package org.alien4cloud.tosca.model.definitions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FunctionParameterPropertyValue<T>  extends AbstractPropertyValue {
  
  protected T value;

}
