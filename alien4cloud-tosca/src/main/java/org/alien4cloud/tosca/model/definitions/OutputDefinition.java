package org.alien4cloud.tosca.model.definitions;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.ui.form.annotation.FormProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

/**
 * A TOSCA output definition.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "name", "description", "value" })
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "name", "description", "value" })
@ToString
public class OutputDefinition implements IValue {

  @StringField(indexType = IndexType.no)
  private String name;

  @StringField(indexType = IndexType.no)
  private String description;

  @ObjectField(enabled = false)
  @JsonDeserialize(using = PropertyValueDeserializer.class)
  private AbstractPropertyValue value;

  @Override
  public boolean isDefinition() {
    return true;
  }
}
