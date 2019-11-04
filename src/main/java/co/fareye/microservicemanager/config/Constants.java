package co.fareye.microservicemanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Constants {




    private static String MICROSERVICE_PROJECT_ID;

    public static String microServiceImageRepository;


    @Value("${MICROSERVICE_PROJECT_ID}")
    public void setMicroServiceProjectId(String microServiceProjectId) {
        MICROSERVICE_PROJECT_ID = microServiceProjectId;
        microServiceImageRepository = "asia.gcr.io/"+MICROSERVICE_PROJECT_ID;
    }

    public static final  String microServiceHpa = "micro-service-hpa";
    public static final  String microServiceName = "micro-service-deployment";


    public static final  String success = "success";
    public static final  String failed = "failed";
    public static final  String pending = "pending";
    public static final  String updateFailed = "update-failed";
    public static final  String deleted = "deleted";
    public static final  String databaseUpdateFailed = "db-failed";
    public static final  String processing = "processing";


    public static final  String microServiceProject = "microServiceProject";

    public static final  String updateEvent = "updateEvent";
    public static final  String createEvent = "createEvent";
    public static final  String backupEvent = "backupEvent";

    public static final  String MICROSERVICE_MANAGER = "MSM"; // For connector environments(integration environment)
    public static final  String MICROSERVICE = "MS";





}
