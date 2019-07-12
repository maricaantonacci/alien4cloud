package org.alien4cloud.tosca.model.definitions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.json.deserializer.PropertyValueDeserializer;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public abstract class PropertyValue<T> extends AbstractPropertyValue {
    @JsonDeserialize(contentUsing = PropertyValueDeserializer.class)
    protected T value;
    
    @Override
    public String toString() {
      return value.toString();
    }
}
