'use strict';

microServiceManagerApp.config(function($stateProvider) {
    $stateProvider
        .state('cluster', {
            templateUrl: "scripts/cluster/views/cluster_creator.html?v=" + version,
            url: "^/cluster",
            parent: "main",
            controller: 'ClusterController',

        });
});