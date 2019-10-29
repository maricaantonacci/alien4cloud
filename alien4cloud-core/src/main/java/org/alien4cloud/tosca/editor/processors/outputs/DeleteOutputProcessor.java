package org.alien4cloud.tosca.editor.processors.outputs;

import alien4cloud.exception.NotFoundException;
import alien4cloud.tosca.context.ToscaContext;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.editor.operations.outputs.DeleteOutputOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.OutputDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

import static alien4cloud.utils.AlienUtils.safe;

@Slf4j
@Component
public class DeleteOutputProcessor  extends AbstractOutputProcessor<DeleteOutputOperation> {


    @Override
    protected boolean create() {
        return false;
    }

    @Override
    protected void processOutputOperation(Csar csar, Topology topology, DeleteOutputOperation operation, Map<String, OutputDefinition> outputs) {
        if (!outputs.containsKey(operation.getOutputName())) {
            throw new NotFoundException("Output " + operation.getOutputName() + "not found in topology");
        }

        //deletePreConfiguredOutput(csar, topology, operation);

        outputs.remove(operation.getOutputName());
        topology.setOutputs(outputs);
        log.debug("Remove the output " + operation.getOutputName() + " from the topology " + topology.getId());
    }

//    protected void deletePreConfiguredOutput(Csar csar, Topology topology, DeleteOutputOperation deleteOutputOperation) {
//        UpdateOutputExpressionOperation updateInputExpressionOperation = new UpdateOutputExpressionOperation();
//        // only set the name, leaving the expression to null, as a null expression is considered as a removal of the pre-conf output entry
//        updateInputExpressionOperation.setName(deleteOutputOperation.getOutputName());
//        updateInputExpressionProcessor.process(csar, topology, updateInputExpressionOperation);
//    }
}
