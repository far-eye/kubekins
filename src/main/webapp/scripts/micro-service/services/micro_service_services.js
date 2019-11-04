
microServiceManagerApp.factory('MicroServiceVersionService', ["$resource", function ($resource) {
      return $resource('app/rest/microServiceVersions', {}, {
        'get': { method: 'GET', isArray: true}
      });
}]);
microServiceManagerApp.factory('MicroServiceEnvironmentListService', ["$resource", function ($resource) {
      return $resource('app/rest/allMicroServicesEnvironments', {}, {
        'get': { method: 'GET', isArray: false  }
      });
}]);

microServiceManagerApp.factory('MicroServiceEnvironmentService', ["$resource", function ($resource) {
      return $resource('app/rest/microServiceEnvironments', {}, {
        'get': { method: 'GET', isArray: true},
        'save': { method: 'POST', isArray: false},
        'deleteEnv': { method: 'DELETE', isArray: false}
      });
}]);
microServiceManagerApp.factory('MicroServiceEnvironmentDtoService', ["$resource", function ($resource) {
      return $resource('app/rest/microServiceEnvironmentDto', {}, {
        'get': { method: 'GET', isArray: true}
        });
}]);
microServiceManagerApp.factory('MicroServiceRegistrationService', ["$resource", function ($resource) {
      return $resource('app/rest/microService', {}, {
        'get': { method: 'GET', isArray: false},
        'getAll': { method: 'GET', isArray: true},
        'save': { method: 'POST', isArray: false},
      });
}]);

microServiceManagerApp.factory('MicroServiceEnvironmentVariableService', ["$resource", function ($resource) {
      return $resource('app/rest/microServiceEnvironmentVariables', {}, {
        'get': { method: 'GET', isArray: true},
      });
}]);

microServiceManagerApp.factory('MicroServiceDomainService', ["$resource", function ($resource) {
      return $resource('app/rest/microServiceDomain', {}, {
        'get': { method: 'GET', isArray: false},
      });
}]);

microServiceManagerApp.factory('MicroServiceDeploymentStepsService', ["$resource", function ($resource) {
      return $resource('app/rest/microService-deployment-steps', {}, {
        'get': { method: 'GET', isArray: true}
      });
}]);

