package org.alien4cloud.tosca.model.definitions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import alien4cloud.ui.form.annotation.FormProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a simple scalar property value.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FormProperties({ "value" })
@ToString(callSuper = true)
@JsonDeserialize(using=alien4cloud.json.deserializer.ScalarPropertyValueDeserializer.class)
@JsonSerialize(using=alien4cloud.json.serializer.ScalarPropertyValueSerializer.class)
public class ScalarPropertyValue extends PropertyValue<String> {

    public ScalarPropertyValue(String value) {
        super(value);
    }
}