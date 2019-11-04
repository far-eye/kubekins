'use strict';

microServiceManagerApp.config(function ($stateProvider, $urlRouterProvider) {
        $stateProvider
            .state('main', {
                templateUrl: 'scripts/core/views/main.html?v=' + version,
                url: '/home',
                controller: 'MainController'

            });

        $urlRouterProvider.otherwise('/home');
    }
);