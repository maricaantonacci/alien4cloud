package org.alien4cloud.tosca.normative.types.datatypes;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;

public class DatatypesNetworkPortDef extends DatatypesPrimitive<Integer> {

    public static final String NAME = "tosca.datatypes.network.PortDef";

    @Override
    public Integer parse(String text) throws InvalidPropertyValueException {
        return Integer.parseInt(text);
    }

    @Override
    public String print(Integer value) {
        return value.toString();
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}


