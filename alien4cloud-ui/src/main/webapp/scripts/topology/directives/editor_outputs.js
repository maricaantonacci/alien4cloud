// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');

  require('scripts/topology/controllers/editor_outputs_edit_modal');
  require('scripts/topology/services/topology_variables_service');
  require('scripts/topology/services/topology_json_processor');
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
    ['$scope', 'topologyVariableService', '$http', 'topoBrowserService', '$filter', '$uibModal', 'topologyJsonProcessor',
    	function($scope, topologyVariableService, $http, topoBrowserService, $filter, $uibModal, topologyJsonProcessor) {



      $scope.dumpOutputValue = function(value) {
        let res = topologyJsonProcessor.renderOuputValue(value);
        let json = JSON.stringify(res, null, ' ');
        json = json.replace(/^"+|(?!\\)(")+$/g, '');
        json = json.replace(/\{\s*\"(.+)\"\s*:/g,"{ $1:").replace(/\n/g, "");//.replace(/]\}/g, " ] }"); 
        return json;
        //return $filter('a4cLinky')(yaml.safeDump(value, {indent: 4}), 'openVarModal');
      };

     function refresh(){
//        var inputsFileNode = topologyVariableService.getInputsNode($scope.topology.archiveContentTree.children[0]);
//        // var inputsFileNode = topologyVariableService.getInputs($scope.topology.archiveContentTree.children[0]);

//        if(_.defined(inputsFileNode)){
//          topoBrowserService.getContent($scope.topology.topology, inputsFileNode, function(result){
//            $scope.loadedOutputs= yaml.safeLoad(result.data);
//          });
//        }
    	$scope.outputsMap = Object.create(null);
    	if ($scope.topology.topology.hasOwnProperty("outputs"))
          Object.keys($scope.topology.topology.outputs).forEach(function(key, index) {
              $scope.outputsMap[key] = $scope.topology.topology.outputs[key];
          });
     }


    $scope.deleteOutput = function(outputName) {
      $scope.execute({
        type: 'org.alien4cloud.tosca.editor.operations.outputs.DeleteOutputOperation',
        outputName: outputName
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
            type: 'org.alien4cloud.tosca.editor.operations.outputs.AddOutputOperation',
            outputName: output.name,
            outputDefinition: output
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
                outputDescription: function() {
                  return $scope.outputsMap[outputName].description;
                },
            outputExpression: function() {
              return _.defined($scope.outputsMap[outputName]) &&
                _.defined($scope.outputsMap[outputName].value) ?
            		  $scope.dumpOutputValue($scope.outputsMap[outputName].value) : '';
            }
          }
        });
        modalInstance.result.then(function(output) {
          // ${app_trigram}/${env_name}/demo
          $scope.execute({
            type: 'org.alien4cloud.tosca.editor.operations.outputs.UpdateOutputOperation',
            outputName: outputName,
            outputDefinition: output
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
       if(_.defined(newValue)){
         refresh();
       }
      });
    }
  ]);
}); // define
