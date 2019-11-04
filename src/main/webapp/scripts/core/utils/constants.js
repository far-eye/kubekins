'use strict';

microServiceManagerApp.constant('STATUS_CONSTANTS', {
   'success':'success',
   'failed':'failed',
   'pending':'pending',
   'updateFailed':'update-failed',
   'deleted':'deleted',
   'databaseUpdateFailed':'db-failed',
   'processing':'processing'
});


microServiceManagerApp.constant('ENVIRONMENT_TYPES', {
    MICROSERVICE : "MS" ,
    MICROSERVICE_MANAGER : "MSM" ,
});