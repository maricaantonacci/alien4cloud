package org.alien4cloud.tosca.model.definitions;

import java.util.List;

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
public class ListPropertyValue extends PropertyValue<List<Object>> {

    public ListPropertyValue(List<Object> value) {
        super(value);
    }
}
