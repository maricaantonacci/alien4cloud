package org.alien4cloud.tosca.editor.processors.outputs;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.outputs.ValidateOutputOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ValidateOutputProcessor  extends AbstractOutputProcessor<ValidateOutputOperation>  {
    @Override
    protected boolean create() {
        return false;
    }

    @Override
    protected void processOutputOperation(Csar csar, Topology topology, ValidateOutputOperation operation, Map<String, OutputDefinition> outputs) {
        if (operation.getOutputName() == null || operation.getOutputName().isEmpty() || !operation.getOutputName().matches("\\w+")) {
            throw new InvalidNameException("newOutputName", operation.getOutputName(), "\\w+");
        }

        if (outputs.containsKey(operation.getOutputName())) {
            throw new AlreadyExistException("An output with the id " + operation.getOutputName() + "already exist in the topology " + topology.getId());
        }

        log.debug("Add a new output [ {} ] for the topology [ {} ].", operation.getOutputName(), topology.getId());
    }
}
