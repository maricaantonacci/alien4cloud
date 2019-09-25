define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/list_to_map_service');

  modules.get('a4c-common').factory('topologyJsonProcessor', ['listToMapService', function(listToMapService) {
      // This service post-process a topology json in order to add maps field from ordered maps array (array of MapEntry).


      function renderToscaFuntion(jsonFunction) {
        var result = Object.create(null);
        var fName = jsonFunction["function"];
        var params = jsonFunction["parameters"];
        var newParams = [];
        for (const param of params) {
            if (typeof param === 'string' || param instanceof String || typeof param == 'number') {
                newParams.push(param);
            } else {
                newParams.push(renderToscaFuntion(param));
            }
        }
        result[fName] = (newParams.length == 1 ? newParams[0] : newParams);
        return result;
      }

      return {

        renderOuputValue: function(outputValue) {
            if (typeof outputValue === 'string' || outputValue instanceof String || typeof outputValue == 'number')
                return outputValue;
             else
                return renderToscaFuntion(outputValue);

        },

        renderToscaFuntion: function(jsonFunction) {
            return renderToscaFuntion(jsonFunction);
        },

        /**
         * Compute the number of times that a requirement template is used by relationships.
         *
         * @param topology The topology dto object to post-process after json deserialization.
         */
        process: function(topology) {
          listToMapService.processMap(topology.nodeTypes, 'properties');
          listToMapService.processMap(topology.policyTypes, 'properties');
          listToMapService.processMap(topology.relationshipTypes, 'properties');
          listToMapService.processMap(topology.capabilityTypes, 'properties');

          this.postProcess(topology.topology.nodeTemplates, ['properties', 'attributes', 'requirements', 'relationships', 'capabilities']);
          this.postProcess(topology.topology.policies, ['properties']);
          
          _.each(topology.topology.nodeTemplates, function(nodeTemplate) {
            nodeTemplate.metadata = {};
            _.each(nodeTemplate.tags, function(tag) {
              nodeTemplate.metadata[tag.name] = tag.value;
            });
            _.each(nodeTemplate.relationships, function(relationship) {
              listToMapService.process(relationship.value, 'properties');
            });
            _.each(nodeTemplate.capabilities, function(capability) {
              listToMapService.process(capability.value, 'properties');
            });
          });
        },

        postProcess: function(object, properties) {
          for (var i = 0; i < properties.length; i++) {
            listToMapService.processMap(object, properties[i]);
          }
        }
      };
    } // function
  ]); // factory
}); // define
