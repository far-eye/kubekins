'use strict';

microServiceManagerApp.config(function($stateProvider) {
    $stateProvider
        .state('microService', {
            templateUrl: "scripts/micro-service/views/micro_service.html?v=" + version,
            url: "^/microService",
            parent: "main",
            controller: 'MicroServiceController'

        }).state('microService.environment', {

            url: "/:microServiceId",
            templateUrl: 'scripts/micro-service/views/micro_service_environment.html?v=' + version,
            controller: 'MicroServiceEnvironmentController',
            params: {
                microServiceId: undefined,
            }

        });

});