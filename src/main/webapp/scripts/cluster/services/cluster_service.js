microServiceManagerApp.factory('ClusterRegistrationService', ["$resource", function ($resource) {
      return $resource('app/rest/cluster', {}, {
        'get': { method: 'GET', isArray: false},
        'getAll': { method: 'GET', isArray: true},
        'save': { method: 'POST', isArray: false},
        'delete': { method: 'DELETE', isArray: false},
        'update': { method: 'PUT', isArray: false}
      });
}]);

microServiceManagerApp.factory('GCloudRegionService', ["$resource", function ($resource) {
      return $resource('app/rest/gcloud_regions', {}, {
        'get': { method: 'GET', isArray: false}
      });
}]);


microServiceManagerApp.factory('UploadKeyService', ["$resource", function ($resource) {
      return $resource('app/rest/upload_file', {}, {
        'save': { method: 'POST', isArray: false}
      });
}]);


microServiceManagerApp.factory('DeleteKeyService', ["$resource", function ($resource) {
      return $resource('app/rest/delete_file', {}, {
        'deleteFiles': { method: 'DELETE', isArray: false}
      });
}]);