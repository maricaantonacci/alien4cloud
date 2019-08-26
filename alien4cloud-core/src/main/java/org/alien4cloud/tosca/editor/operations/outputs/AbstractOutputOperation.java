package org.alien4cloud.tosca.editor.operations.outputs;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

public abstract class AbstractOutputOperation extends AbstractEditorOperation {
    /** The name of the output to add/remove or rename in the topology. */
    protected String outputName;
}
