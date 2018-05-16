define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');
  require('scripts/deployment/directives/display_outputs');

  states.state('applications.detail.environment.deploycurrent.executions', {
    url: '/executions',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_executions.html',
    controller: 'ApplicationEnvDeployCurrentExecutionsCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.executions',
      state: 'applications.detail.environment.deploycurrent.executions',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_EXEC',
      icon: 'fa fa-film',
      priority: 400
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentExecutionsCtrl',
  ['$scope', 'applicationServices', 'application', '$state','breadcrumbsService', '$translate', 'searchServiceFactory',
  function($scope, applicationServices, applicationResult, $state, breadcrumbsService, $translate, searchServiceFactory) {

    breadcrumbsService.putConfig({
      state : 'applications.detail.environment.deploycurrent.executions',
      text: function(){
        return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_EXEC');
      },
      onClick: function(){
        $state.go('applications.detail.environment.deploycurrent.executions');
      }
    });


    $scope.displayLogs = function(executionId) {
      $state.go('applications.detail.environment.deploycurrent.logs', {
        'applicationId': $scope.application.id,
        'applicationEnvironmentId': $scope.environment.id,
        'executionId': executionId
      });
    };

    $scope.applicationServices = applicationServices;
    $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;
    $scope.executionStatusIconCss = alienUtils.getExecutionStatusIconCss;
    $scope.executionStatusTextCss = alienUtils.getExecutionStatusTextCss;

    $scope.application = applicationResult.data;

    $scope.now = new Date();

    var searchServiceUrl = 'rest/latest/executions/search';
    $scope.queryManager = {
      query: ''
    };


    applicationServices.getActiveDeployment.get({
      applicationId: $scope.application.id,
      applicationEnvironmentId: $scope.environment.id
    }, undefined, function(success) {
      if (_.defined(success.data)) {
        $scope.activeDeployment = success.data;
        $scope.searchService = searchServiceFactory(searchServiceUrl, true, $scope.queryManager, 30, 50, true, null, { deploymentId: $scope.activeDeployment.id });
        $scope.queryManager.onSearchCompleted = function(searchResult) {
          $scope.executions = searchResult.data.data;
        };
        $scope.searchService.search();
      }
    });

  }
]);
});
