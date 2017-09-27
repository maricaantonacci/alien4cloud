package org.alien4cloud.tosca.editor.processors.workflow;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.workflow.AddActivityOperation;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Process the {@link AddActivityOperation} operation
 */
@Slf4j
@Component
public class AddActivityProcessor extends AbstractWorkflowProcessor<AddActivityOperation> {

    @Override
    protected void processWorkflowOperation(AddActivityOperation operation, Workflow workflow) {
        Topology topology = EditionContextManager.getTopology();
        log.debug("adding activity <{}> to the workflow <{}> from topology <{}>", operation.getActivity().toString(), workflow.getName(), topology.getId());
        workflowBuilderService.addActivity(topology, EditionContextManager.getCsar(), operation.getWorkflowName(), operation.getRelatedStepId(),
                operation.isBefore(), operation.getActivity());
    }
}
