package org.alien4cloud.tosca.normative.types.datatypes;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.types.IPropertyType;

public class DatatypesRoot<T> implements IPropertyType<T> {
    public static final String NAME = "tosca.datatypes.Root";


    @Override
    public T parse(String text) throws InvalidPropertyValueException {
        return null;
    }

    @Override
    public String print(T value) {
        return value.toString();
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
