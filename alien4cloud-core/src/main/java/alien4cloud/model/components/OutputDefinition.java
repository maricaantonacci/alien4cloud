package alien4cloud.model.components;

import javax.validation.constraints.NotNull;

import lombok.*;

import alien4cloud.ui.form.annotation.FormProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "value", "description" })
@JsonIgnoreProperties(ignoreUnknown = true)
@FormProperties({ "value", "description" })
@ToString
public class OutputDefinition implements IValue {
    
    @NotNull
    private AbstractPropertyValue value;

    private String description;

    @Override
    public boolean isDefinition() {
        return false;
    }

}
