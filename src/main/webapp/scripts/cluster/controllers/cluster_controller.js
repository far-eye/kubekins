'use strict';

microServiceManagerApp.controller('ClusterController', function($http, $state, $scope, $rootScope, $timeout,ClusterRegistrationService,GCloudRegionService, UploadKeyService, DeleteKeyService) {

    $scope.clusterMap = {};
    $scope.pageSize = 10;
    $scope.retry = false;
    $scope.update = false;

    $scope.clearCluster = function(){
         $scope.cluster = null;
         $scope.update=false;
         $scope.saveClusterModalForm.$setPristine();
    }

    $scope.pageNumber = 0;
    $scope.pageDetails = {
            'sortOn': 'id',
            'sortType': 'ASC',
            'pageNo': 0,
            'recordsPerPage': 10,
            'query': ''
        };


    $scope.setPageSize = function() {
             $scope.pageDetails.recordsPerPage = $scope.pageSize;
             $scope.getClusters();
         };

    $scope.sortOn = function(sortOn) {
            if ($scope.pageDetails.sortType == "DESC") {
                $scope.pageDetails.sortType = "ASC";
            } else {
                $scope.pageDetails.sortType = "DESC";
            }
            $scope.pageDetails.sortOn = sortOn;
            $scope.getClusters();
        };

     $scope.pageChanged = function(pageNumber) {
             $scope.pageDetails.pageNo = pageNumber - 1;
             $scope.pageNumber = pageNumber - 1;
             $scope.getClusters();
         };

    $scope.refresh = function() {
            $scope.getClusters();
        }

    $scope.createCluster = function(){
//        console.log($scope.cluster);
        $scope.startAjaxRequestHandler();
        if(!($scope.validateForm())){
            $scope.errorResponseHandler("Please select Certificate and Key.");
        }else{
            $scope.errorMessage=$scope.customValidationForSpace($scope.cluster);
            if($scope.errorMessage != null){
                $scope.errorResponseHandler($scope.errorMessage);
            }else{
                if($scope.update == false){
                $scope.uploadFile($scope.cluster.clustername);
                    ClusterRegistrationService.save({}, $scope.cluster, function(data) {
                        $scope.successResponseHandler("Cluster registered!");
                        $('#saveClusterModal').modal('hide');
                        $scope.clearCluster();
                        $scope.getClusters();
                    }, function(httpResponse) {
                        $scope.errorResponseHandler(httpResponse.data.message);
                    });
                }else if($scope.update == true){
                     $scope.uploadFile($scope.cluster.clustername);
                     ClusterRegistrationService.update({}, $scope.cluster, function(data) {
                                     $scope.successResponseHandler("Cluster modified!");
                                     $('#saveClusterModal').modal('hide');
                                     $scope.clearCluster();
                                     $scope.getClusters();
                                 }, function(httpResponse) {
                                     $scope.errorResponseHandler(httpResponse.data.message);
                                 });
                }
            }
        }

    }


    $scope.validateForm=function(){
        if($scope.update == true){
            return true;
        }else{
            if(document.getElementById('tlsCert').files[0] == undefined || document.getElementById('tlsKey').files[0] == undefined){
                return false;
            }else{
                return true;
            }
        }
    }


    $scope.edit = function(id, showModel) {
            $scope.clearCluster();
            $scope.readOnly = false;
            $scope.cluster = angular.copy($scope.clusterMap[id]);

            if (showModel) {
                $scope.update=true;
                $('#saveClusterModal').modal('toggle');
            } else {
                $scope.createCluster();
                $timeout($scope.refresh, 2000);
            }

        }


    $scope.deleteCluster = function(id) {
                $scope.startAjaxRequestHandler();

                ClusterRegistrationService.delete({
                    id:id
                }, function(success) {
                    $scope.successResponseHandler("Successfully deleted " + $scope.clusterMap[id].clustername)
                    $scope.getClusters();

                }, function(httpResponse) {
                    $scope.errorResponseHandler(httpResponse.data.message);
                });
                DeleteKeyService.deleteFiles({
                    clusterName: $scope.clusterMap[id].clustername
                }, $scope.clusterMap[id].clustername,  function(success) {
                    $scope.successResponseHandler("Successfully deleted ")
                    $scope.getClusters();

                }, function(httpResponse) {
                    $scope.errorResponseHandler(httpResponse.data.message);
                });
    }

    $scope.getClusters = function(){
        $scope.clusterList = ClusterRegistrationService.getAll({}, function(data) {
            if ($scope.clusterList.length==0) {
                $scope.noClusters = true;
            } else {
                $scope.noClusters = false;
            }

          $scope.clusterMap = {};
                    if($scope.noClusters==false){
                        $scope.clusterList.forEach(function(clstr) {
                            $scope.clusterMap[clstr["id"]] = {};
                            $scope.clusterMap[clstr["id"]] = clstr;
                        });
                    }
                }, function(error) {});
    }


    $scope.setFlags = function(update, retry, readOnly) {
            $scope.update = update;
            $scope.retry = retry;
            if (readOnly != null && readOnly != undefined) {
                $scope.readOnly = readOnly;
            }
        }

    $scope.getRegions = function() {
            GCloudRegionService.get({}, function(data) {
                 $scope.regionMap = data;
            }, function(error) {
        })};


    $scope.uploadFile=function(clustername){
        var formdata=new FormData();
        formdata.append("uploadedCert",document.getElementById('tlsCert').files[0]);
        formdata.append("uploadedKey",document.getElementById('tlsKey').files[0]);
        formdata.append("clustername",clustername);
        console.log(formdata.get("uploadedCert"));
        console.log(formdata.get("uploadedKey"));

        $http.post("app/rest/upload_file", formdata, {
                        transformRequest: angular.identity,
                        headers: {'Content-Type': undefined}

                    }).success(function (data) {
                        $('#message').html("Files uploaded successfully.");
                        $('#addConfigButton').prop('disabled', true);
                        $('#AddConfiguration').modal();
                        $('#AddConfiguration').modal('open');

                    });
    }

    $scope.customValidationForSpace=function(cluster) {
        if(cluster.projectid.indexOf(' ') >= 0)
            return "Project Id should not contain any space characters";
        if(cluster.clustername.indexOf(' ') >= 0)
             return "Cluster Name should not contain any space characters";
        if(cluster.domainName.indexOf(' ') >= 0)
            return "Domain name should not contain any space characters";

         return null;
    }

    $scope.getRegions();
    $scope.getClusters();

});