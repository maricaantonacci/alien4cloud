package org.alien4cloud.tosca.editor.processors.nodetemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation;
import org.alien4cloud.tosca.editor.processors.FileProcessorHelper;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.stereotype.Component;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;

import org.alien4cloud.tosca.utils.TopologyUtils;

/**
 * Process an {@link UpdateNodeDeploymentArtifactOperation}.
 */
@Component
public class UpdateNodeDeploymentArtifactProcessor implements IEditorOperationProcessor<UpdateNodeDeploymentArtifactOperation> {
    @Override
    public void process(Csar csar, Topology topology, UpdateNodeDeploymentArtifactOperation operation) {
        // Get the node template's artifacts to update
        Map<String, NodeTemplate> nodeTemplates = TopologyUtils.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = TopologyUtils.getNodeTemplate(topology.getId(), operation.getNodeName(), nodeTemplates);
        DeploymentArtifact artifact = nodeTemplate.getArtifacts() == null ? null : nodeTemplate.getArtifacts().get(operation.getArtifactName());
        if (artifact == null) {
            throw new NotFoundException("Artifact with key [" + operation.getArtifactName() + "] do not exist");
        }

        if (operation.getArtifactRepository() == null) {
            // this is an archive file, ensure that the file exists within the archive
            if (operation.getArtifactReference() != null && Files.exists(Paths.get(operation.getArtifactReference())))
                FileProcessorHelper.getFileTreeNode(operation.getArtifactReference());
            artifact.setArtifactRepository(ArtifactRepositoryConstants.VIRTUAL_ARTIFACT_REPOSITORY);
            artifact.setRepositoryName(null);
            artifact.setRepositoryURL(null);
        } else {
            artifact.setArtifactRepository(operation.getArtifactRepository());
            artifact.setRepositoryName(operation.getRepositoryName());
            artifact.setRepositoryURL(operation.getRepositoryUrl());
        }

        //if (operation.getArtifactReference() != null && Files.exists(Paths.get(operation.getArtifactReference())))
        artifact.setArtifactRef(operation.getArtifactReference());
        artifact.setArchiveName(operation.getArchiveName());
        artifact.setArchiveVersion(operation.getArchiveVersion());
        nodeTemplate.getArtifacts().replace(artifact.getArtifactName(), artifact);
        nodeTemplates.replace(operation.getNodeName(), nodeTemplate);
        topology.setNodeTemplates(nodeTemplates);
    }
}