microServiceManagerApp.controller('MainController', function($scope,$rootScope,$timeout) {

    console.log("test")
    $scope.closeAlert = function() {
        $scope.success = false;
        $scope.error = false;
        $scope.message = "";

    }

    $scope.closeAlert();



     $scope.errorResponseHandler = function(message) {

        $scope.message = message;
        $scope.enableButtonClick();
        $scope.success = false;
        $scope.error = true;
        $rootScope.setLoader(false);
        $timeout( function(){
           $scope.closeAlert();
        }, 5000 );

    }


    $scope.successResponseHandler = function(message) {

        $scope.message = message;
        $scope.enableButtonClick();
        $scope.success = true;
        $scope.error = false;
        $rootScope.setLoader(false);
        $timeout( function(){
            $scope.closeAlert();
        }, 5000 );

    }

    $scope.startAjaxRequestHandler = function() {
        $scope.disableButtonClick();
        $rootScope.setLoader(true);
    }


    $scope.deleteMethod = null;
    $scope.deleteElement = null;
    $scope.deleteStub = function(method,element){
        $scope.deleteMethod = method;
        $scope.deleteElement = element;
        $('#deleteModal').modal('toggle');
    }

    $scope.delete = function(){

       $scope.deleteMethod($scope.deleteElement);
       $('#deleteModal').modal('hide');

    }

});
