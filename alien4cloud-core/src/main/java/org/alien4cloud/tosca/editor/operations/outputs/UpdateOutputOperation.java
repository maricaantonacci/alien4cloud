package org.alien4cloud.tosca.editor.operations.outputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;

@Setter
@Getter
public class UpdateOutputOperation  extends AbstractOutputOperation {

    /**
     * The definition of the output we want to add
     */
    protected OutputDefinition outputDefinition;

    @Override
    public String commitMessage() {
        return "update output";
    }
}
