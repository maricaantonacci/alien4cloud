/** Manage artifacts upload and reset for topology edition. */
define(function (require) {
  'use strict';
  var modules = require('modules');

  require('scripts/topology/controllers/editor_artifact_new_edit');

  modules.get('a4c-topology-editor').factory('topoEditArtifactsNewEdit', [ '$uibModal', 'topologyServices',
    function($uibModal, topologyServices) {
      var TopologyEditorNewEditMixin = function(scope) {
        this.scope = scope;
        //this.scope.
      };


      TopologyEditorNewEditMixin.prototype = {
        constructor: TopologyEditorNewEditMixin,
        addNewArtifact: function() {

                              var modalInstance = $uibModal.open({
                                templateUrl: 'views/topology/editor_artifacts_new_edit.html',
                                controller: 'TopologyEditorArtifactNewEditCtrl',
                                resolve: {
                                  archiveContentTree: function() {
                                    return null;//scope.topology.archiveContentTree;
                                  },
                                  availableRepositories: function() {
                                    return null;//scope.availableRepositories;
                                  },
                                  topology: function(){
                                    return null;
                                  },
                                  artifact: function(){
                                    return Object.create(null);
                                  },
                                  isCreateNew: function() {
                                    return true;
                                  }

                                }
                              });

                              modalInstance.result.then(function(artifact) {
                                scope.execute({
                                    type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation',

                                  }
                                );
                              });
                        },

        /**
        * This method is triggered when the user select the artifact
        */
//        onSelect: function(artifactName, artifact) {
//          var scope = this.scope;
//          topologyServices.availableRepositories({
//            topologyId: scope.topologyId
//          }, function(result) {
//              scope.availableRepositories = result.data;
//              var modalInstance = $uibModal.open({
//                templateUrl: 'views/topology/editor_artifact_new_edit.html',
//                controller: 'TopologyEditorArtifactNewEditCtrl',
//                resolve: {
//                  archiveContentTree: function() {
//                    return scope.topology.archiveContentTree;
//                  },
//                  availableRepositories: function() {
//                    return scope.availableRepositories;
//                  },
//                  artifact: function() {
//                    return artifact;
//                  },
//                  topology: function(){
//                    return scope.topology.topology;
//                  }
//                }
//              });
//
//              modalInstance.result.then(function(artifact) {
//                scope.execute({
//                    type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.UpdateNodeDeploymentArtifactOperation',
//                    nodeName: scope.selectedNodeTemplate.name,
//                    artifactName: artifactName,
//                    artifactReference: artifact.reference,
//                    artifactRepository: artifact.repository,
//                    repositoryUrl: artifact.repositoryUrl,
//                    repositoryName: artifact.repositoryName,
//                    archiveName: artifact.archiveName,
//                    archiveVersion: artifact.archiveVersion
//                  }
//                );
//              });
//            }
//          );
//
//        },
//
//        // reset the artifact to the original value from node type
//        reset: function(artifactName) {
//          this.scope.execute({
//              type: 'org.alien4cloud.tosca.editor.operations.nodetemplate.ResetNodeDeploymentArtifactOperation',
//              nodeName: this.scope.selectedNodeTemplate.name,
//              artifactName: artifactName
//            }
//          );
//        }
      };

      return function(scope) {
        var instance = new TopologyEditorNewEditMixin(scope);
        scope.artifactsNewEdit = instance;
      };
    }
  ]); // modules
}); // define
