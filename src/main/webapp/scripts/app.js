'use strict';

/* App Module */

var microServiceManagerApp = angular.module('microServiceManagerApp', [ 'ui.router','ngResource','angularUtils.directives.dirPagination','mgcrea.ngStrap','checklist-model','AxelSoft']);

microServiceManagerApp.config(function($logProvider){
    $logProvider.debugEnabled(false);
});