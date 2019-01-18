package alien4cloud.model.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract class for a value that doesn't have a property definition (such as scalar value or a function value).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractPropertyValue implements IValue, IPrintable {

    private boolean printable;

    @Override
    public boolean isDefinition() {
        return false;
    }
}