package org.alien4cloud.tosca.editor.operations.outputs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteOutputOperation extends AbstractOutputOperation {

    @Override
    public String commitMessage() {
        return "delete output";
    }
}
