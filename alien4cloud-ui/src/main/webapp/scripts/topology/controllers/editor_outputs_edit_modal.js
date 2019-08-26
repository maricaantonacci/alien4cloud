// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var yaml = require('js-yaml');
  var angular = require('angular');

  modules.get('a4c-topology-editor', ['a4c-common', 'ui.ace', 'treeControl']).controller('a4cEditorOutputExpEditCtrl',
    ['$scope', '$uibModal', 'isCreateNew', 'outputName', 'outputDescription', 'outputExpression', '$uibModalInstance', 'propertiesServices',
      function($scope,  $uibModal, isCreateNew, outputName, outputDescription, outputExpression, $uibModalInstance, propertiesServices) {
        $scope.outputName = outputName;
        $scope.outputDescription = outputDescription;
        $scope.isCreateNew = isCreateNew;

        $scope.outputExpression = {
          str: outputExpression,
          obj: yaml.safeLoad(outputExpression)
        };

        $scope.getPropertyDefinition = function() {
          return $scope.topology.topology.outputs[outputName];
        };

        $scope.updateLocalExpression = function(def, name, value) {
          $scope.outputExpression.str = yaml.safeDump(value, {indent: 4});
        };

        $scope.ok = function() {
          $uibModalInstance.close(
          {
            outputName: $scope.outputName,
            outputDescription: "test",
            //$scope.outputDescription,
            outputExpression: $scope.outputExpression
            });
        };

        $scope.cancel = function() {
          $uibModalInstance.dismiss('canceled');
        };

        $scope.typeMatch = false;
        if(_.definedPath($scope, 'outputExpression.obj')){
          propertiesServices.validConstraints({}, angular.toJson({
            'definitionId': $scope.outputName,
            'propertyDefinition': $scope.getPropertyDefinition(),
            'dependencies': $scope.topology.topology.dependencies,
            'value': $scope.outputExpression.obj
          }), function(successResult) {
            if(_.get(successResult, 'error.code') === 804) {
              $scope.typeMatch = false;
              $scope.activeTab = 1;
              return;
            }
            $scope.typeMatch = true;
          });
        }
      }
    ]);
});
