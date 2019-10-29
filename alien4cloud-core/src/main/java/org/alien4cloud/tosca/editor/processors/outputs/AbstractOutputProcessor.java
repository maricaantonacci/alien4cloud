package org.alien4cloud.tosca.editor.processors.outputs;

import alien4cloud.exception.NotFoundException;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.editor.operations.outputs.AbstractOutputOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Topology;

import java.util.Map;

public abstract class AbstractOutputProcessor<T extends AbstractOutputOperation> implements IEditorOperationProcessor<T> {
    @Override
    public void process(Csar csar, Topology topology, T operation) {
        Map<String, OutputDefinition> outputs = topology.getOutputs();
        if (outputs == null) {
            if (create()) {
                outputs = Maps.newHashMap();
            } else {
                throw new NotFoundException("The topology has no defined input");
            }
        }
        processOutputOperation(csar, topology, operation, outputs);
    }

    /**
     * If true then the inputs map will be created rather than throwing an exception.
     *
     * @return true if we should create the input map if none exists.
     */
    protected abstract boolean create();

    protected abstract void processOutputOperation(Csar csar, Topology topology, T operation, Map<String, OutputDefinition> outputs);
}
