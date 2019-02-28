package org.alien4cloud.tosca.model.definitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.tosca.container.validation.ToscaPropertyConstraint;
import alien4cloud.tosca.container.validation.ToscaPropertyDefaultValueType;
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
@ToscaPropertyDefaultValueType
@ToscaPropertyConstraint
@EqualsAndHashCode(of = { "name" })
@FormProperties({ "name", "description", "value" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutputDefinition<T>  implements IValue {
  
  protected String name;
  
  protected String description = "";

  @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
  protected T value;

  @Override
  @JsonIgnore
  public boolean isDefinition() {
    return true;
  }

}
