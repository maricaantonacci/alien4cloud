package org.alien4cloud.tosca.editor.operations.outputs;

import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

@Getter
@Setter
public abstract class AbstractOutputOperation extends AbstractEditorOperation {
    /** The name of the output to add/remove or rename in the topology. */
    protected String outputName;
}
