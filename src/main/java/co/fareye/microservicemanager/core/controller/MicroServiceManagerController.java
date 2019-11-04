package co.fareye.microservicemanager.core.controller;


import co.fareye.microservicemanager.config.Constants;
import co.fareye.microservicemanager.core.domain.MicroService;
import co.fareye.microservicemanager.core.domain.MicroServiceEnvironment;
import co.fareye.microservicemanager.core.dto.ApiResponseMessage;
import co.fareye.microservicemanager.core.dto.MicroServiceDto;
import co.fareye.microservicemanager.core.dto.MicroServiceEnvironmentDto;
import co.fareye.microservicemanager.core.dto.PageDetailsDTO;
import co.fareye.microservicemanager.core.service.MicroServiceManager;
import co.fareye.microservicemanager.core.service.UtilityService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/app")
public class MicroServiceManagerController {

    private final Logger log = LoggerFactory.getLogger(MicroServiceManagerController.class);

    @Inject
    private MicroServiceManager microServiceManagerService;

    @Inject
    private UtilityService utilityService;

    @Value("${MICROSERVICE_DOMAIN_NAME}")
    private String MICROSERVICE_DOMAIN_NAME;

    @RequestMapping(value = "/rest/microService",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMicroServices(@RequestParam(value = "code", required = false) String code, @RequestParam(value = "id", required = false) Long id) throws IOException {

        if(code == null && id == null) {
            List<MicroService> microServices = microServiceManagerService.getAllMicroServices();
            return new ResponseEntity<>(microServices, HttpStatus.OK);
        }else{

            MicroService microService;
            if(code!=null) {
                microService  = microServiceManagerService.getMicroServiceByCode(code);
            }else{
                microService  = microServiceManagerService.getMicroServiceById(id);
            }
            if(microService == null){
                return new ResponseEntity<>(new ApiResponseMessage("MicroService with given code/id doesn't exist"), HttpStatus.BAD_REQUEST);

            }
            return new ResponseEntity<>(microService, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/rest/microServiceDomain",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDomain()  {
        return new ResponseEntity<>(new ApiResponseMessage(MICROSERVICE_DOMAIN_NAME), HttpStatus.OK);
    }


    @RequestMapping(value = "/rest/microService",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveMicroService(@RequestBody MicroServiceDto microServiceDto) {


        String errorMessage;

        if(microServiceDto.getCode()==null || !microServiceDto.getCode().matches("[a-z]([-a-z0-9]*[a-z0-9])?")){
            errorMessage = "code must consist of lower case alphanumeric characters and start with non number";
            return new ResponseEntity<>(new ApiResponseMessage(errorMessage),HttpStatus.BAD_REQUEST);
        }
        try{
            MicroService microService = microServiceManagerService.saveMicroService(microServiceDto);
            if(microService==null) {
                errorMessage = "Could not register with codeship";
                return new ResponseEntity<>(new ApiResponseMessage(errorMessage), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(microService,HttpStatus.OK);

        }  catch(Exception e)    {
            log.error("Exception: " , e);
            return new ResponseEntity<>(new ApiResponseMessage(e.getMessage()),HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/rest/allMicroServicesEnvironments",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MicroServiceEnvironment>> getList(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                 @RequestParam(value = "recordsPerPage", required = false) Integer recordsPerPage,
                                                                 @RequestParam(value = "sortOn", required = false) String sortOn,
                                                                 @RequestParam(value = "sortType", required = false) String sortType,
                                                                 @RequestParam(value = "query", required = false) String query,
                                                                 @RequestParam(value = "microServiceId", required = false) Long microServiceId) throws IOException {
        log.debug("REST request to get All Environment List");
        PageDetailsDTO pageDetailsDTO = new PageDetailsDTO(pageNo, recordsPerPage, sortOn, sortType);
        if (query == null || query.equals("")) {
            query = "";
        }
        return microServiceManagerService.getAllMicroServicesEnvironmentPageable(pageDetailsDTO, query,microServiceId);
    }


    @RequestMapping(value = "/rest/microServiceEnvironments",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMicroServiceEnvironments(@RequestParam(value = "microServiceId", required = true) Long microServiceId)       {

        List<MicroServiceEnvironment> microServiceEnvironmentList = microServiceManagerService.getMicroServiceEnvironmentByMicroServiceId(microServiceId);

        return new ResponseEntity<>(microServiceEnvironmentList, HttpStatus.OK);
    }

    @RequestMapping(value = "/rest/microServiceEnvironmentDto",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMicroServiceEnvironments()       {

        List<MicroServiceEnvironmentDto> microServiceEnvironmentList = microServiceManagerService.getAllMicroServiceEnvironmentsDto();

        return new ResponseEntity<>(microServiceEnvironmentList, HttpStatus.OK);
    }


    @RequestMapping(value = "/rest/microServiceEnvironments",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveMicroServiceEnvironment(@RequestBody MicroServiceEnvironment environment, @RequestParam(value = "retry", required = false ,defaultValue = "false") Boolean retry, @RequestParam(value = "update", required = false ,defaultValue = "false") Boolean update, @RequestParam(value = "microServiceId", required = true) Long microServiceId) {

        environment.setCreationDate(DateTime.now());
        environment.setLastModifiedDate(DateTime.now());
        environment.setStatus(Constants.pending);

        String errorMessage ="";

        if(environment.getCode()==null || !environment.getCode().matches("[a-z]([-a-z0-9]*[a-z0-9])?")){
            errorMessage = "code must consist of lower case alphanumeric characters and start with non number";
            return new ResponseEntity<>(new ApiResponseMessage(errorMessage),HttpStatus.BAD_REQUEST);
        }

        MicroService microService = microServiceManagerService.getMicroServiceById(microServiceId);
        environment.setMicroService(microService);
        if(microService==null){
            errorMessage = "MicroService not found!";
            return new ResponseEntity<>(new ApiResponseMessage(errorMessage),HttpStatus.BAD_REQUEST);
        }
        try{

            Boolean versionCheck = false;
            List<String> versionList = microServiceManagerService.listOfVersions("microService_"+microService.getCode());
            for( String version : versionList){
                if(utilityService.compareVersion(version,environment.getVersion())==0){
                    versionCheck = true;
                    break;
                }
            }
            if(environment.getVersion()!=null && versionCheck ){

                List<String> errorList = microServiceManagerService.checkEnvironmentVariables(environment.getEnvironmentVariables(),"microService_"+microService.getCode()+"/"+environment.getVersion());
                if(errorList.size()>0) {
                    for(int x =0 ; x < errorList.size(); x++){
                        errorMessage += (x+1)+". " + errorList.get(x) + "  ";
                    }
                    return new ResponseEntity<>(new ApiResponseMessage(errorMessage), HttpStatus.BAD_REQUEST);
                }
                environment = microServiceManagerService.saveMicroServiceEnvironment(environment, retry,update);

            }else{
                errorMessage = "version doesn't exist!";
                return new ResponseEntity<>(new ApiResponseMessage(errorMessage),HttpStatus.BAD_REQUEST);

            }
            return new ResponseEntity<>(environment,HttpStatus.OK);

        }  catch(Exception e)    {
            log.error("Exception: " , e);
            environment.setLogs(e.getMessage());
            return new ResponseEntity<>(new ApiResponseMessage(environment.getLogs()),HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/rest/microServiceEnvironments",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteMicroServiceEnvironment(@RequestParam(value = "code") String code)       {


        try {
            String message = microServiceManagerService.deleteEnvironment(code);
            return new ResponseEntity<>( new ApiResponseMessage(message), HttpStatus.OK);

        }catch(Exception e) {
            log.error("Exception: ", e);
            return new ResponseEntity<>(new ApiResponseMessage(e.getMessage()), HttpStatus.CONFLICT);
        }
    }


    @RequestMapping(value = "/rest/microServiceVersions",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMicroServiceEnvironmentVersion(@RequestParam(value = "code") String code)       {

        List<String> integrationEnvironments = microServiceManagerService.listOfVersions("microService_"+code);
        return new ResponseEntity<>(integrationEnvironments, HttpStatus.OK);
    }

    @RequestMapping(value = "/rest/microServiceEnvironmentVariables",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getEnvironmentVariables(@RequestParam(value = "version") String version ,@RequestParam(value = "code") String microServiceCode){

        try {
            List<String> environmentVariables = microServiceManagerService.getUserDefinedEnvironmentVariableList("microService_"+microServiceCode+"/"+version);
            return new ResponseEntity<>(environmentVariables, HttpStatus.OK);

        }catch (Exception e) {
            return new ResponseEntity<>(new ApiResponseMessage("Environment variables for versions not found"), HttpStatus.BAD_REQUEST);
        }
    }


}
