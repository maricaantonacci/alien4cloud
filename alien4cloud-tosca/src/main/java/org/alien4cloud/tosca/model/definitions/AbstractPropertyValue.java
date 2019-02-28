package org.alien4cloud.tosca.model.definitions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.NoArgsConstructor;

/**
 * Abstract class for a value that doesn't have a property definition (such as scalar value or a function value).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public abstract class AbstractPropertyValue implements IValue {

    @Override
    public boolean isDefinition() {
        return false;
    }
}