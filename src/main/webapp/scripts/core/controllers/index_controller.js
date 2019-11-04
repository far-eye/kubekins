microServiceManagerApp.controller('IndexController', function($scope,$rootScope) {

    $scope.disableButtonClick = function () {
        $scope.isDisabledClick = true;
    };
    $scope.enableButtonClick = function () {
        $scope.isDisabledClick = false;
    };

    $rootScope.setLoader = function(flag){
        $rootScope.showLoader = flag;
    }

});