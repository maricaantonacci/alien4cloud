// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');

  require('scripts/topology/controllers/editor_outputs_edit_modal');
  require('scripts/topology/services/topology_variables_service');
  require('scripts/topology/services/topology_browser_service');
  require('scripts/topology/directives/variable_display_ctrl');
  require('scripts/common/filters/a4c_linky');

  modules.get('a4c-topology-editor').directive('editorOutputs',
    [
    function() {
      return {
        restrict: 'E',
        templateUrl: 'views/topology/outputs/editor_outputs.html',
        controller: 'editorOutputsCtrl'
      };
    }
  ]); // directive


  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('editorOutputsCtrl',
    ['$scope', 'topologyVariableService', '$http', 'topoBrowserService', '$filter', '$uibModal', 
    	function($scope, topologyVariableService, $http, topoBrowserService, $filter, $uibModal) {

      $scope.dump = function(value) {
        return $filter('a4cLinky')(yaml.safeDump(value, {indent: 4}), 'openVarModal');
      };

      function refresh(){
        var inputsFileNode = topologyVariableService.getInputsNode($scope.topology.archiveContentTree.children[0]);
        // var inputsFileNode = topologyVariableService.getInputs($scope.topology.archiveContentTree.children[0]);

        if(_.defined(inputsFileNode)){
          topoBrowserService.getContent($scope.topology.topology, inputsFileNode, function(result){
            $scope.loadedOutputs= yaml.safeLoad(result.data);
          });
        }
      }

      $scope.clearOutput = function(outputName) {
        $scope.execute({
          type: 'org.alien4cloud.tosca.editor.operations.outputs.UpdateOutputExpressionOperation',
          name: outputName,
          description: null,
          expression: null
        });
      };

      $scope.createOutput = function() {
    	  var modalInstance = $uibModal.open({
              templateUrl: 'views/topology/outputs/edit_output_modal.html',
              controller: 'a4cEditorOutputExpEditCtrl',
              scope: $scope,
              size: 'lg',
              resolve: {
                isCreateNew: function() {
                    return true;
                },
                outputName: function() {
                  return "";
                },
                outputDescription: function() {
                  return "";
                },
                outputExpression: function() {
                  return "";
                }
              }
            });
        modalInstance.result.then(function(output) {
          // ${app_trigram}/${env_name}/demo
          $scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.outputs.UpdateOutputExpressionOperation',
            name: output.outputName,
            description: output.outputDescription,
            expression: output.outputExpression.str
          });
        });
      }
      
      $scope.editOutput = function(outputName) {
        var modalInstance = $uibModal.open({
          templateUrl: 'views/topology/outputs/edit_output_modal.html',
          controller: 'a4cEditorOutputExpEditCtrl',
          scope: $scope,
          size: 'lg',
          resolve: {
                isCreateNew: function() {
                    return false;
                },
            outputName: function() {
              return outputName;
            },
            outputExpression: function() {
              return _.defined($scope.loadedOutputs) && _.defined($scope.loadedOutputs[outputName]) ? 
            		  yaml.safeDump($scope.loadedOutputs[outputName], {indent: 4}) : '';
            }
          }
        });
        modalInstance.result.then(function(outputExpression) {
          // ${app_trigram}/${env_name}/demo
          $scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.outputs.UpdateOutputExpressionOperation',
            name: outputName,
            expression: outputExpression.str
          });
        });
      };

      $scope.openVarModal = function(varName){
        $uibModal.open({
          templateUrl: 'views/topology/variables/variable_display.html',
          controller: 'variableDisplayCtrl',
          scope: $scope,
          size: 'lg',
          resolve: {
            varName: function() {
              return varName;
            }
          }
        });
      };

      $scope.$watch('triggerVarRefresh', function(newValue){
//        if(_.defined(newValue)){
//          refresh();
//        }
      });
    }
  ]);
}); // define
