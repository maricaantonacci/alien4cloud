package org.alien4cloud.tosca.editor.operations.outputs;

public class UpdateOutputExpressionOperation extends AbstractOutputOperation {

    @Override
    public String commitMessage() {
        return "update output " + outputName;
    }
}
