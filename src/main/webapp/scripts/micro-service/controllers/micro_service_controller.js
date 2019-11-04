'use strict';

microServiceManagerApp.controller('MicroServiceController', function($http, $state, $scope, $rootScope, $timeout,MicroServiceRegistrationService) {

    $scope.microServiceMap = {};
    $scope.clearMicroService = function(){

         $scope.microService = null;
         $scope.registerMicroServiceModalForm.$setPristine();
    }

    $scope.createMicroService = function(){

        $scope.startAjaxRequestHandler();
        MicroServiceRegistrationService.save({}, $scope.microService, function(data) {

            $scope.successResponseHandler("MicroService registered!");
            $('#registerMicroServiceModal').modal('hide');
            $scope.clearMicroService();
            $scope.getMicroServices();
        }, function(httpResponse) {
            $scope.errorResponseHandler(httpResponse.data.message);
        });

    }

    $scope.getMicroServices = function(){
        $scope.microServiceList = MicroServiceRegistrationService.getAll({}, function(data) {

            if (data.length==0) {
                $scope.noMicroServices = true;
            } else {
                $scope.noMicroServices = false;
            }

        }, function(error) {});
    }

    $scope.getMicroServices();

});