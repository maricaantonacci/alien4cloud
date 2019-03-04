package org.alien4cloud.tosca.model.definitions;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ComplexPropertyValue extends PropertyValue<Map<String, Object>> {

    public ComplexPropertyValue(Map<String, Object> value) {
        super(value);
    }
    
    @Override
    public boolean isDefinition() {
        return false;
    }
}
