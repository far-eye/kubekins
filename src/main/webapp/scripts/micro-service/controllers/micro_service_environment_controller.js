'use strict';

microServiceManagerApp.controller('MicroServiceEnvironmentController', function($http, $state, $scope, $rootScope, $timeout,$stateParams, MicroServiceVersionService,MicroServiceEnvironmentListService,MicroServiceEnvironmentService,MicroServiceEnvironmentVariableService,MicroServiceRegistrationService,ClusterRegistrationService, STATUS_CONSTANTS,MicroServiceDeploymentStepsService,MicroServiceDomainService) {

    $scope.STATUS_CONSTANTS = STATUS_CONSTANTS;
    $scope.environmentVariableList = []
    var microServiceId = $stateParams.microServiceId;

    $scope.pageDetails = {
        'sortOn': 'id',
        'sortType': 'ASC',
        'pageNo': 0,
        'recordsPerPage': 10,
        'query': ''
    };

    $scope.pageSize = 10;
    $scope.noContentRow = false;
    $scope.retry = false;
    $scope.update = false;
    $scope.noMicroServices = false;

    $scope.sortOn = function(sortOn) {
        if ($scope.pageDetails.sortType == "DESC") {
            $scope.pageDetails.sortType = "ASC";
        } else {
            $scope.pageDetails.sortType = "DESC";
        }
        $scope.pageDetails.sortOn = sortOn;
        $scope.getRecords();
    };


    $scope.setPageSize = function() {
        $scope.pageDetails.recordsPerPage = $scope.pageSize;
        $scope.getRecords();
    };

    $scope.pageNumber = 0;
    $scope.pageChanged = function(pageNumber) {
        $scope.pageDetails.pageNo = pageNumber - 1;
        $scope.pageNumber = pageNumber - 1;
        $scope.getRecords();
    };

    var environmentIdMap = {}

    $scope.getRecords = function() {

        $scope.environmentList = MicroServiceEnvironmentListService.get({microServiceId : microServiceId },$scope.pageDetails, function(data) {
            $scope.records = data.numberOfElements;
            $scope.totalRecords = data.totalElements;
            if (data.numberOfElements == 0) {
                $scope.noContentRow = true;
            } else {
                $scope.noContentRow = false;
            }
            environmentIdMap = {};

            if($scope.noContentRow==false){
                $scope.environmentList.content.forEach(function(env) {

                    environmentIdMap[env.id] = {};
                    environmentIdMap[env.id] = env;
                });
            }

        }, function(error) {});

    };

    $scope.environment = {}

    $scope.clear = function() {
        $scope.doNotMatch = null;
        $scope.saveEnvironmentModalForm.$setPristine();
        $scope.saveEnvironmentModalForm.confirmPassword = null;
        $scope.environment = null;

    }

//    MicroServiceDomainService.get({}, function(data) {
//                    $scope.microServiceDomain = data.message;
//
//                }, function(error) {
//                    $scope.microServiceDomain = "fareyeconnect.com";
//    });

    $scope.refresh = function() {
        MicroServiceRegistrationService.get({
                id: microServiceId
            }, function(data) {
                $scope.selectedMicroService = data;
                MicroServiceVersionService.get({
                    code: data.code
                }, function(successData) {
                    $scope.allVersions = successData;
                }, function(error) {});
                $scope.sortOn('creationDate');

            }, function(error) {

            });
    }

    $scope.create = function() {

        if($scope.environment.status && ($scope.environment.status==STATUS_CONSTANTS.failed || $scope.environment.status==STATUS_CONSTANTS.deleted)){
              $scope.update = false;
              $scope.retry = true;
        }

        $scope.startAjaxRequestHandler();
        MicroServiceEnvironmentService.save({
            update: $scope.update,
            retry: $scope.retry,
            microServiceId: microServiceId
        }, $scope.environment, function(data) {

            $scope.successResponseHandler("Environment creation/updation started!");
            $('#saveEnvironmentModal').modal('hide');
            $scope.clear();
            $scope.getRecords($scope.pageSize);
        }, function(httpResponse) {
            $scope.errorResponseHandler(httpResponse.data.message);
        });
    }


    $scope.edit = function(id, showModel) {
        $scope.clear();
        $scope.readOnly = true;
        $scope.environment = angular.copy(environmentIdMap[id]);

        if (showModel) {
            $('#saveEnvironmentModal').modal('toggle');
        } else {
            $scope.create();
            $timeout($scope.refresh, 2000);
        }

    }

    $scope.deleteEnv = function(code) {

        $scope.startAjaxRequestHandler();
        MicroServiceEnvironmentService.deleteEnv({
            code: code
        }, function(success) {
            $scope.successResponseHandler("Successfully deleted environment with code " + code)
            $scope.getRecords();

        }, function(httpResponse) {
            $scope.errorResponseHandler(httpResponse.data.message);
        });
    }

    $scope.setFlags = function(update, retry, readOnly) {
        $scope.update = update;
        $scope.retry = retry;
        if (readOnly != null && readOnly != undefined) {
            $scope.readOnly = readOnly;
        }
    }

    $scope.environmentVariableListMap = {};
    var versionWiseEnvironmentVariableCache = {}
    $scope.getEnvironmentVariables = function() {

        if (!versionWiseEnvironmentVariableCache[$scope.environment.version]) {
            $scope.startAjaxRequestHandler();
            MicroServiceEnvironmentVariableService.get({
                    version: $scope.environment.version,
                    code: $scope.selectedMicroService.code
                }, function(data) {
                    if (!data) {
                        return;
                    }
                    $scope.environmentVariableList = data;
                    $scope.environmentVariableListMap={};

                    for (var x = 0; x < data.length; x++) {
                        $scope.environmentVariableListMap[data[x]] = "";
                    }

                    versionWiseEnvironmentVariableCache[$scope.environment.version] = angular.copy($scope.environmentVariableListMap);
                    prepareMap();
                },
                function(error) {
                    if (error.data.message)
                        $scope.errorResponseHandler(error.data.message);
                    return;
                });
        } else {
            $scope.environmentVariableListMap = {}
            $scope.environmentVariableListMap = angular.copy(versionWiseEnvironmentVariableCache[$scope.environment.version]);
            prepareMap();

        }
    }
    var prepareMap = function() {

        $scope.environment.environmentVariables = angular.fromJson($scope.environment.environmentVariables);
        angular.forEach($scope.environment.environmentVariables, function(value, key) {
            if ($scope.environmentVariableListMap[key]!=null && $scope.environmentVariableListMap[key]!=undefined) {
                $scope.environmentVariableListMap[key] = value;
            }
        });
        $scope.environment.environmentVariables = angular.toJson($scope.environment.environmentVariables);
        $scope.enableButtonClick();
        $rootScope.setLoader(false);
    }

    $scope.saveEnvironmentVariableList = function() {
        $scope.environment.environmentVariables = angular.toJson($scope.environmentVariableListMap);
        $('#environmentVariableModal').modal('toggle');
    }

    $scope.getDeploymentSteps = function(id,version1,version2){

            $scope.selectedEnvironmentId =  id;
            $scope.selectedVersion =  version2;
            if(!version1){
                version1 = version2;
            }
            $scope.startAjaxRequestHandler();
            MicroServiceDeploymentStepsService.get({code :$scope.selectedMicroService.code , version1 : version1 , version2 : version2}, function(data) {
                   $("#deploymentStepsModal").modal('show');
                   $scope.enableButtonClick();
                   $rootScope.setLoader(false);
                   $scope.deploymentSteps = data;
            }, function(httpResponse) {
                $scope.errorResponseHandler(httpResponse.data[0]);
            });
    }


    var getMicroServiceData = function(){

           $scope.enableButtonClick();
            $rootScope.setLoader(false);
          $scope.refresh();
    }

    $scope.clusterMap = {}
    $scope.getClusters = function(){
                    $scope.clusterList = ClusterRegistrationService.getAll({}, function(data) {
                        if ($scope.clusterList.length==0) {
                            $scope.noClusters = true;
                        } else {
                            $scope.noClusters = false;
                            for(var x = 0 ; x < $scope.clusterList.length; x++){
                                $scope.clusterMap[$scope.clusterList[x].id] = $scope.clusterList[x]
                            }
                        }
                });
            }
            $scope.getClusters();

    getMicroServiceData();

});