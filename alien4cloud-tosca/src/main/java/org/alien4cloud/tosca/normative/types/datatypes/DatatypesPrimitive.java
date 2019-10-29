package org.alien4cloud.tosca.normative.types.datatypes;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;
import org.alien4cloud.tosca.normative.types.IPropertyType;

public abstract class DatatypesPrimitive<T>  implements IPropertyType<T> {

    protected T value;

    public abstract T parse(String text) throws InvalidPropertyValueException;

    public abstract String print(T value);

    public abstract String getTypeName();
}
