package org.alien4cloud.tosca.editor.operations.outputs;

public class AddOutputOperation extends AbstractOutputOperation {

    protected String outputDescription;
    protected String outputExpression;

    @Override
    public String commitMessage() {
        return "add output";
    }
}
