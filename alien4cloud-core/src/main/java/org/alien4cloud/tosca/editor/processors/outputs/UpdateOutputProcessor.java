package org.alien4cloud.tosca.editor.processors.outputs;

import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidNameException;
import alien4cloud.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.outputs.UpdateOutputOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class UpdateOutputProcessor  extends AbstractOutputProcessor<UpdateOutputOperation>  {

    @Override
    protected boolean create() {
        return true; // create the outputs map if null in the topology.
    }

    @Override
    protected void processOutputOperation(Csar csar, Topology topology,
                                          UpdateOutputOperation operation,
                                          Map<String, OutputDefinition> outputs) {
        if (checkOperationName(operation.getOutputName())) {
            throw new InvalidNameException("oldOutputName", operation.getOutputName(), "\\w+");
        }

        if (checkOperationName(operation.getOutputDefinition().getName())) {
            throw new InvalidNameException("New output name ", operation.getOutputDefinition().getName(), "\\w+");
        }

        if (!outputs.containsKey(operation.getOutputName())) {
            throw new NotFoundException("An output with the id " + operation.getOutputName() + " doesn't exist in the topology " + topology.getId());
        }

        if (!outputs.containsKey(operation.getOutputDefinition().getName())) {
            throw new AlreadyExistException("An output with the id "
                    + operation.getOutputDefinition().getName()
                    + " already exists in the topology " + topology.getId());
        }

        OutputDefinition od = outputs.get(operation.getOutputName());
        outputs.remove(operation.getOutputName());
        outputs.put(operation.getOutputDefinition().getName(), operation.getOutputDefinition());

        //outputs.put(operation.getOutputName(), operation.getOutputDefinition());
        topology.setOutputs(outputs);

        log.debug("Update output with old name [ {} ] for the topology [ {} ].", operation.getOutputName(), topology.getId());
    }

    protected boolean checkOperationName(String operationName) {
        return operationName == null || operationName.isEmpty() || !operationName.matches("\\w+");
    }
}
