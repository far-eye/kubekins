package co.fareye.microservicemanager.core.service;

import co.fareye.microservicemanager.clusterManager.domain.Cluster;
import co.fareye.microservicemanager.clusterManager.dto.ClusterFilesDTO;
import co.fareye.microservicemanager.clusterManager.repository.ClusterManagerRepository;
import co.fareye.microservicemanager.config.Constants;
import co.fareye.microservicemanager.config.Exceptions.ScriptException;
import co.fareye.microservicemanager.config.aqmp.MicroServiceQueue;
import co.fareye.microservicemanager.core.domain.MicroService;
import co.fareye.microservicemanager.core.domain.MicroServiceEnvironment;
import co.fareye.microservicemanager.core.dto.MicroServiceDto;
import co.fareye.microservicemanager.core.dto.MicroServiceEnvironmentDto;
import co.fareye.microservicemanager.core.dto.MicroServiceRabbitMqDto;
import co.fareye.microservicemanager.core.dto.PageDetailsDTO;
import co.fareye.microservicemanager.core.repository.MicroServiceEnvironmentRepository;
import co.fareye.microservicemanager.core.repository.MicroServiceManagerRepository;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.*;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class MicroServiceManager {

    private final Logger logger = LoggerFactory.getLogger(MicroServiceManager.class);

    @Value("${MICROSERVICE_YAML_FOLDER_PATH}")
    private String MICROSERVICE_YAML_FOLDER_PATH;

    @Value("${MICROSERVICE_PROJECT_ID}")
    private String MICROSERVICE_PROJECT_ID;

    @Value("${CODESHIP_USERNAME}")
    private String CODESHIP_USERNAME;

    @Value("${CODESHIP_PASSWORD}")
    private String CODESHIP_PASSWORD;

    @Value("${NEW_RELIC_LICENCE_KEY}")
    private String NEW_RELIC_LICENCE_KEY;

    @Value("${HOME_PATH}")
    private String HOME_PATH;

    private String versionToDeploy;

    private String versionNumberString;

    private String microServiceImageName;

    private String microServiceYamlFolderPathWithMicroServiceCode;

    private  final String  AFFINITY_LABEL =  "affinity-label";


    private Map<String, List<String>> imagesVersionMap;

    @Inject
    private S3DownloadService s3DownloadService;

    @Inject
    private MicroServiceManagerRepository microServiceManagerRepository;

    @Inject
    private ClusterManagerRepository clusterManagerRepository;

    @Inject
    private MicroServiceEnvironmentRepository microServiceEnvironmentRepository;

    @Inject
    private RabbitTemplate rabbitTemplate;

    @Inject
    private MicroServiceQueue environmentQueue;


    @Inject
    private S3UrlService s3UrlService;


    @Inject
    private TransferManager transferManager;


    @Inject
    private UtilityService utilityService;


    public List<MicroService> getAllMicroServices() {
        return microServiceManagerRepository.findAll();
    }

    public MicroServiceEnvironment saveMicroServiceEnvironment(MicroServiceEnvironment microServiceEnvironment, Boolean retry, Boolean update) throws Exception {

        MicroServiceEnvironment microServiceEnvironmentOld = microServiceEnvironmentRepository.getByCode(microServiceEnvironment.getCode());
        if(update){
            if(microServiceEnvironmentOld!=null){
                if(microServiceEnvironmentOld.getStatus().equals(Constants.success) || microServiceEnvironmentOld.getStatus().equals(Constants.updateFailed)  || microServiceEnvironment.getStatus().equals(Constants.databaseUpdateFailed) ){
                    microServiceEnvironment.setId(microServiceEnvironmentOld.getId());

                    // Cannot update this property
                    microServiceEnvironment.setKeepEnvironmentDataAfterDeletion(microServiceEnvironmentOld.getKeepEnvironmentDataAfterDeletion());

                    microServiceEnvironment.setStatus(Constants.pending);
                    microServiceEnvironment.setCreationDate(microServiceEnvironmentOld.getCreationDate());
                    microServiceEnvironment.setOldVersion(microServiceEnvironmentOld.getVersion());


                }else{
                    throw new Exception("Environment not ready! Try again later");
                }

                microServiceEnvironment = microServiceEnvironmentRepository.save(microServiceEnvironment);
                MicroServiceRabbitMqDto microServiceRabbitMqDto = new MicroServiceRabbitMqDto(microServiceEnvironment,microServiceEnvironmentOld,Constants.updateEvent,Constants.microServiceProject);
                rabbitTemplate.convertAndSend(environmentQueue.getQueueName(),microServiceRabbitMqDto);

                return microServiceEnvironment;
            }else{
                throw new Exception("Code does not exists!");
            }
        }else {
            if (microServiceEnvironmentOld == null || retry) {


                if (microServiceEnvironmentOld != null) {
                    microServiceEnvironment.setId(microServiceEnvironmentOld.getId());
                    if (microServiceEnvironment.getStatus().equals(Constants.success)) {
                        throw new Exception("Environment already deployed!");
                    }
                }
                microServiceManagerRepository.increaseEnvironmentCount(microServiceEnvironment.getMicroService().getCode());

                microServiceEnvironment = microServiceEnvironmentRepository.save(microServiceEnvironment);
                MicroServiceRabbitMqDto microServiceRabbitMqDto = new MicroServiceRabbitMqDto(microServiceEnvironment,null,Constants.createEvent,Constants.microServiceProject);

                rabbitTemplate.convertAndSend(environmentQueue.getQueueName(), microServiceRabbitMqDto);

                return microServiceEnvironment;
            } else {
                throw new Exception("Code already exists!");
            }
        }
    }

    public String createMicroServiceEnvironment(MicroServiceEnvironment microServiceEnvironment) throws Exception {

        String namespace = microServiceEnvironment.getCode();
        String version = microServiceEnvironment.getVersion();
        versionToDeploy = version+"/";
        versionNumberString = version.replace("v","");
        microServiceImageName = Constants.microServiceImageRepository+"/microService_"+microServiceEnvironment.getMicroService().getCode();
        microServiceYamlFolderPathWithMicroServiceCode = MICROSERVICE_YAML_FOLDER_PATH+"microService_"+microServiceEnvironment.getMicroService().getCode()+"/";
        String result = "";
        co.fareye.microservicemanager.clusterManager.domain.Cluster  clstr=clusterManagerRepository.getByClusterId(microServiceEnvironment.getClusterid());
        if(clstr != null) {
            utilityService.authenticateWithKubectl(clstr.getProjectid(), clstr.getClustername(), clstr.getRegion());
        }else{
            logger.error("Cluster not selected/found");
            microServiceEnvironment.setStatus(Constants.failed);
            microServiceEnvironment.setLogs("Cluster not selected/found");
            microServiceEnvironment = microServiceEnvironmentRepository.save(microServiceEnvironment);
            return microServiceEnvironment.getStatus();

        }


        utilityService.executeCommands("kubectl get pods",true,1);

        Config config = new Config();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            try {
                logger.info("namespace : " + namespace+" --> "+"creating namespace!");
                client.namespaces().createNew()
                        .withNewMetadata()
                        .withName(namespace)
                        .endMetadata()
                        .done();

                s3DownloadService.downloadFolder("microService_"+microServiceEnvironment.getMicroService().getCode()+"/"+microServiceEnvironment.getVersion(),MICROSERVICE_YAML_FOLDER_PATH,s3UrlService.getS3MicroServiceBucketName());

                imagesVersionMap = getAllVersionsOfImages(Constants.microServiceImageRepository+"/microService_"+microServiceEnvironment.getMicroService().getCode());

                logger.info("namespace : " + namespace+" --> "+"Loading all configuration files");

                File file = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"ingress/ingress.yaml");

                logger.info("namespace : " + namespace+" --> "+"applying ingress");
                InputStream targetStream = new FileInputStream(file);

                List ingressList  = client.load(targetStream).inNamespace(namespace).get();
                Ingress ingress = (Ingress) ingressList.get(0);
                ingress.getMetadata().getAnnotations().put("kubernetes.io/ingress.global-static-ip-name","microService-ip");
                ingress.getSpec().getRules().get(0).setHost(namespace+"."+clstr.getDomainName());
                ingress.getSpec().getTls().get(0).getHosts().set(0,namespace+"."+clstr.getDomainName());
                client.extensions().ingresses().create(ingress);


                ClusterFilesDTO keyFile=utilityService.getClusterFiles(HOME_PATH+"/tmp"+"/cluster/"+clstr.getClustername(), "key");
                ClusterFilesDTO certFile=utilityService.getClusterFiles(HOME_PATH+"/tmp"+"/cluster/"+clstr.getClustername(), "cert");
                utilityService.executeCommands("kubectl create secret tls tls-certificate --key "+keyFile.getFilePath()+ " --cert "+ certFile.getFilePath() +" --namespace="+namespace,true,1);

                logger.info("namespace : " + namespace+" --> "+"certificate applied successfully");

                Map<String, Set<String>> envVarKeys = setAllEnvironmentVariables(namespace,client,microServiceEnvironment);

                loadConfigFiles(namespace, client,false, clstr);
                loadAllServiceFiles(namespace, client);


                deployMicroServiceEnvironment(namespace, client,microServiceEnvironment,envVarKeys);


                logger.info("namespace : " + namespace+" --> "+" pod disruption budget");

                utilityService.executeCommands("printf '\n  namespace: "+namespace+"' >> "+microServiceYamlFolderPathWithMicroServiceCode +versionToDeploy+"pdb.yaml",true,1);
                utilityService.executeCommands("kubectl create -f "+microServiceYamlFolderPathWithMicroServiceCode +versionToDeploy+"pdb.yaml",true,1);

                microServiceEnvironment.setStatus(Constants.success);
                microServiceEnvironment.setLogs("");

                logger.info("namespace : " + namespace+" --> "+"microService deployed waiting for services to get ready");


                // changing pvc to retain from delete
                if(microServiceEnvironment.getKeepEnvironmentDataAfterDeletion()) {
                    result = utilityService.executeCommands("kubectl get pv | grep " + namespace + " | cut -d \" \" -f1\n", true, 1);
                    String[] pvcList = result.split("\n");
                    for (int x = 0; x < pvcList.length; x++) {
                        utilityService.executeCommands("kubectl patch pv " + pvcList[x] + " -p '{\"spec\":{\"persistentVolumeReclaimPolicy\":\"Retain\"}}'", true, 1);
                    }
                }
            }catch (Exception e){
                logger.error("Exception: " , e);

                //clean up resources
                client.namespaces().withName(namespace).delete();
                client.close();
                microServiceEnvironment.setStatus(Constants.failed);
                microServiceEnvironment.setLogs(e.getMessage());
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            microServiceEnvironment.setStatus(Constants.failed);
            microServiceEnvironment.setLogs(e1.getMessage());

        }
        try {
            microServiceEnvironment = microServiceEnvironmentRepository.save(microServiceEnvironment);
            utilityService.executeCommands("rm -rf " + MICROSERVICE_YAML_FOLDER_PATH + "/" + "microService_" + microServiceEnvironment.getMicroService().getCode() + "/" + microServiceEnvironment.getVersion(), true, 1);
        }catch (Exception e){
            e.printStackTrace();

        }
        return microServiceEnvironment.getStatus();
    }

    private Map<String, Set<String>> setAllEnvironmentVariables(String namespace, KubernetesClient client, MicroServiceEnvironment microServiceEnvironment) throws FileNotFoundException {

        File file = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"environment-variables.yaml");

        logger.info("namespace : " + namespace+" --> "+"applying environment variables");
        InputStream targetStream = new FileInputStream(file);

        List configList  = client.load(targetStream).inNamespace(namespace).get();
        ConfigMap config = (ConfigMap) configList.get(0);
//        config.getData().put("PG_PASSWORD",password);
        config.getData().put("ENVIRONMENT",namespace);



        Map<String,String> environmentVariableMap = getEnvironmentVariablesMap(microServiceEnvironment.getEnvironmentVariables());
        if(environmentVariableMap!=null) {
            for (Map.Entry<String,String> entry :  environmentVariableMap.entrySet()){
                config.getData().put(entry.getKey(),entry.getValue());
            }
        }
        Map<String ,Set<String>> environmentVariablesKeys = new LinkedHashMap<>();
        environmentVariablesKeys.put(config.getMetadata().getName(),config.getData().keySet());
        client.configMaps().create(config);

        logger.info("namespace : " + namespace+" --> "+"environment variable loaded");
        return environmentVariablesKeys;
    }


    private String deployMicroServiceEnvironment(String namespace, KubernetesClient client, MicroServiceEnvironment microServiceEnvironment, Map<String, Set<String>> envVarKeys) throws FileNotFoundException, InterruptedException {

        File file = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"microService/"+Constants.microServiceName+".yaml");

        logger.info("namespace : " + namespace+" --> "+"applying "+ Constants.microServiceName);
        InputStream targetStream = new FileInputStream(file);

        final CountDownLatch closeLatch = new CountDownLatch(1);


        try (Watch watch = client.extensions().deployments().inNamespace(namespace).watch(new Watcher<Deployment>() {
            @Override
            public void eventReceived(Action action, Deployment resource) {
                logger.info("namespace : " + namespace+" --> "+"{} : {} : {}", action, resource.getMetadata().getName(),Constants.microServiceName);
                if (resource.getMetadata().getName().equals(Constants.microServiceName)  && resource.getStatus().getReadyReplicas()!=null && resource.getStatus().getReplicas()!=null && resource.getStatus().getReadyReplicas().equals(resource.getStatus().getReplicas())) {
                    logger.info("namespace : " + namespace+" --> "+Constants.microServiceName+": "+closeLatch.getCount());
                    closeLatch.countDown();
                }
            }

            @Override
            public void onClose(KubernetesClientException e) {
                if (e != null) {
                    logger.error("Exception ", e);
                    while(closeLatch.getCount()!=0){
                        closeLatch.countDown();
                    }
                    throw e;
                }
            }
        })) {

            logger.info("namespace : " + namespace + " --> " + "Waiting for "+Constants.microServiceName+" to start");

            Deployment deployment = null;
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try {
                deployment = mapper.readValue(targetStream, Deployment.class);
                deployment.getMetadata().setNamespace(namespace);
            } catch (Exception e) {
                logger.error("Exception: " , e);
                List deploymentList = client.load(targetStream).inNamespace(namespace).get();
                deployment = (Deployment) deploymentList.get(0);
            }


            List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();

            List<EnvVar> envVars = new ArrayList<>();
            envVarKeys.forEach((String configMapName, Set<String> value) -> {
                for (String envVariableName : value) {
                    ConfigMapKeySelector configMapKeySelector = new ConfigMapKeySelector();
                    configMapKeySelector.setKey(envVariableName);
                    configMapKeySelector.setName(configMapName);
                    EnvVarSource envVarSource = new EnvVarSource();
                    envVarSource.setConfigMapKeyRef(configMapKeySelector);
                    EnvVar envVar = new EnvVar();
                    envVar.setName(envVariableName);
                    envVar.setValueFrom(envVarSource);
                    envVars.add(envVar);
                }

            });



            for (Container container : containers) {
                if (container.getName().contains(Constants.microServiceName)) {
                    container.getEnv().addAll(envVars);
                    container.setImage(microServiceImageName + ":" + utilityService.getVersion(imagesVersionMap.get(microServiceImageName), microServiceImageName, versionNumberString));
//                    container.getResources().getRequests().put("cpu",new Quantity("10m"));
                    enableNewRelic(microServiceEnvironment,container);
                }
            }

            deployment.getSpec().setReplicas(1); // only one replica at the start so flyway migration is applied by only one server. Increase after creation
            //deployment.getSpec().getSelector().getMatchLabels().put(AFFINITY_LABEL,Constants.microServiceName+"-"+microServiceEnvironment.getCode());
            deployment.getSpec().getSelector().getMatchLabels().remove(AFFINITY_LABEL);
            deployment.getSpec().getTemplate().getMetadata().getLabels().put(AFFINITY_LABEL,Constants.microServiceName+"-"+microServiceEnvironment.getCode());
            applyAffinity(microServiceEnvironment,deployment);


            client.extensions().deployments().create(deployment);


            Integer time = 60*microServiceEnvironment.getTargetPods();
            closeLatch.await(time, TimeUnit.SECONDS);


            logger.info("namespace : " + namespace + " --> " + Constants.success);
            if(microServiceEnvironment.getAutoscale()){
                createHpa(microServiceEnvironment,namespace,client,Constants.microServiceName,Constants.microServiceHpa);
            }else if(!microServiceEnvironment.getTargetPods().equals(1)) {
                client.extensions().deployments().inNamespace(namespace).withName(Constants.microServiceName).scale(microServiceEnvironment.getTargetPods(), true);
            }


            return Constants.success;
        }

    }


    private void loadAllServiceFiles(String namespace, KubernetesClient client) throws FileNotFoundException {


        File folder = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"service");
        File[] listOfFiles = folder.listFiles();
        InputStream targetStream;

        if(listOfFiles==null){
            logger.info("namespace : " + namespace+" --> "+"no service files found");
            return;
        }

        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                if (listOfFile.getName().contains(".yaml")) {
                    try {
                        logger.info("namespace : " + namespace + " --> " + "applying " + listOfFile.getName());
                        targetStream = new FileInputStream(listOfFile);

                        List serviceList = client.load(targetStream).inNamespace(namespace).get();
                        io.fabric8.kubernetes.api.model.Service service = (io.fabric8.kubernetes.api.model.Service) serviceList.get(0);
                        client.services().create(service);

                        logger.info("namespace : " + namespace + " --> " + Constants.success);
                    }catch(Exception e){
                        logger.error("Exception e",e);
                    }
                }
            }
        }
    }



    /**
     * Apply all the config maps and secrets used in the environment
     * @param namespace
     * @param client
     * @param replace this parameter is true when already configurations exist and new configurations are to be replaced
     * @param clstr
     * @throws FileNotFoundException
     */
    private void loadConfigFiles(String namespace, KubernetesClient client, Boolean replace, co.fareye.microservicemanager.clusterManager.domain.Cluster clstr) throws FileNotFoundException {


        File folder = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"config");
        File[] listOfFiles = folder.listFiles();
        InputStream targetStream;
        if(listOfFiles==null){
            logger.info("namespace : " + namespace+" --> "+"no configuration files found");
            return;
        }

        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                if (listOfFile.getName().contains(".yaml")) {
                    logger.info("namespace : " + namespace + " --> " + "applying " + listOfFile.getName());
                    targetStream = new FileInputStream(listOfFile);
                    try {
                        List configList = client.load(targetStream).inNamespace(namespace).get();
                        if (configList.get(0).toString().contains("ConfigMap")) {
                            ConfigMap config = (ConfigMap) configList.get(0);

                            if(config.getMetadata().getName().contains("newrelic")){
                                config.getData().put("newrelic.yml",config.getData().get("newrelic.yml").replaceAll("replace_application_name","microService_"+namespace+"_"+clstr.getDomainName()));
                                config.getData().put("newrelic.yml",config.getData().get("newrelic.yml").replaceAll("replace_licence_key",NEW_RELIC_LICENCE_KEY));
                            }
                            if (replace) {
                                client.configMaps().inNamespace(namespace).withName(config.getMetadata().getName()).replace(config);
                            } else {
                                client.configMaps().create(config);
                            }
                        } else if (configList.get(0).toString().contains("Secret")) {
                            Secret secret = (Secret) configList.get(0);
                            if (replace) {
                                client.secrets().inNamespace(namespace).withName(secret.getMetadata().getName()).replace(secret);
                            } else {
                                client.secrets().create(secret);
                            }
                        }
                    }catch (Exception e){
                        logger.error("Exception e",e);
                    }
                }
            }
        }
        logger.info("namespace : " + namespace+" --> "+Constants.success);
    }


    // TODO Google cloud specific , Write other implementations for Amazon or Azure
    private Map<String, List<String>> getAllVersionsOfImages(String code) throws InterruptedException, ScriptException, IOException {


        Map<String,List<String>> imageListVersionMap = new HashMap<>();

        logger.info("getting version of image : "+microServiceImageName);
        String command="gcloud container images list-tags "+code+" --format=\"list(TAGS)\" | cut -f3 -d \" \"";
        String output = utilityService.executeCommands(command,true,1);
        String versionlist[] = output.split("[\\r\\n]+");
        List<String> versions = new ArrayList<>();

        for (String aVersionlist : versionlist) {
            String tags[] = aVersionlist.split(",");
            for (String tag : tags) {

                if (tag.matches("^v[\\d|\\.]+$")) {
                    versions.add(tag);
                    logger.info("version:" + tag);
                }
            }
        }
        versions = utilityService.sortVersions(versions);
        imageListVersionMap.put(microServiceImageName,versions);

        logger.info("done");

        return imageListVersionMap;
    }



    public String deleteEnvironment(String code) throws ScriptException, IOException, InterruptedException {
        MicroServiceEnvironment microServiceEnvironmentOld = microServiceEnvironmentRepository.getByCode(code);
        co.fareye.microservicemanager.clusterManager.domain.Cluster clstr=clusterManagerRepository.getByClusterId(microServiceEnvironmentOld.getClusterid());
        if(clstr != null){
            utilityService.authenticateWithKubectl(clstr.getProjectid(),clstr.getClustername(),clstr.getRegion());
        }else{
            logger.error("Cluster not selected/found");
            throw new RuntimeException("Cluster not selected/found");
        }

        if(microServiceEnvironmentOld ==null){
            throw new RuntimeException("Environment not found!");
        }else if(microServiceEnvironmentOld.getStatus().equalsIgnoreCase("deleted")){
            throw new RuntimeException("Environment already deleted");
        }
        microServiceEnvironmentOld.setStatus(Constants.deleted);
        microServiceEnvironmentOld.setLastModifiedDate(DateTime.now());

        utilityService.executeCommands("kubectl delete ingress ingress --namespace="+code,true,1);
        Thread.sleep(2000);
        String message = utilityService.executeCommands("kubectl delete namespace "+code,true,1);
        microServiceEnvironmentRepository.save(microServiceEnvironmentOld);

        return message;
    }


    public String updateMicroServiceEnvironment(MicroServiceEnvironment microServiceEnvironmentNew, MicroServiceEnvironment microServiceEnvironmentOld) throws Exception {

        String namespace = microServiceEnvironmentNew.getCode();
        String versionNew = microServiceEnvironmentNew.getVersion();
        String versionOld = microServiceEnvironmentOld.getVersion();

        versionToDeploy = versionNew+"/";
        versionNumberString = versionNew.replace("v","");
        microServiceImageName =  Constants.microServiceImageRepository+"/microService_"+microServiceEnvironmentOld.getMicroService().getCode();
        microServiceYamlFolderPathWithMicroServiceCode = MICROSERVICE_YAML_FOLDER_PATH+"microService_"+microServiceEnvironmentOld.getMicroService().getCode()+"/";
        Boolean error= false;

        imagesVersionMap = getAllVersionsOfImages(Constants.microServiceImageRepository+"/microService_"+microServiceEnvironmentOld.getMicroService().getCode());

        s3DownloadService.downloadFolder("microService_"+microServiceEnvironmentOld.getMicroService().getCode()+"/"+microServiceEnvironmentNew.getVersion(),MICROSERVICE_YAML_FOLDER_PATH,s3UrlService.getS3MicroServiceBucketName());


        String deployment = Constants.microServiceName ;

        ConfigMap oldConfig;
        ConfigMap newConfig;
        Cluster clstr=clusterManagerRepository.getByClusterId(microServiceEnvironmentNew.getClusterid());
        if(clstr != null) {
            utilityService.authenticateWithKubectl(clstr.getProjectid(), clstr.getClustername(), clstr.getRegion());
        }else{
            logger.error("Cluster not selected/found");
            microServiceEnvironmentOld.setStatus(Constants.updateFailed);
            microServiceEnvironmentOld.setLogs("Cluster not selected/found");
            microServiceEnvironmentOld = microServiceEnvironmentRepository.save(microServiceEnvironmentOld);
            return microServiceEnvironmentOld.getStatus();
        }

        utilityService.executeCommands("kubectl get pods",true,1);
        Config config = new Config();
        try (final KubernetesClient client = new DefaultKubernetesClient(config)) {

            loadConfigFiles(namespace,client,true, clstr);
            Map<String,ConfigMap> configMaps;
            configMaps = checkChangeInEnvironmentVariables(microServiceEnvironmentNew, microServiceEnvironmentOld , client);
            Boolean hasEnvironmentVariablesChanged;

            if(configMaps.get("new")==null){
                oldConfig = configMaps.get("old");
                newConfig = oldConfig;
                hasEnvironmentVariablesChanged = false;
            }else {
                oldConfig = configMaps.get("old");
                newConfig = configMaps.get("new");
                hasEnvironmentVariablesChanged = true;
            }
            // When versions are not equal , then perform rolling update
            if (utilityService.compareVersion(versionOld,versionNew)!=0 || hasEnvironmentVariablesChanged
                    || (microServiceEnvironmentOld.getNodeAffinity()!=null && (microServiceEnvironmentNew.getNodeAffinity()!=microServiceEnvironmentOld.getNodeAffinity()))
                    || (microServiceEnvironmentNew.getEnableNewRelic()!=microServiceEnvironmentOld.getEnableNewRelic())
            ) {
                try {

                    File file = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"microService/"+Constants.microServiceName+".yaml");
                    InputStream targetStream = new FileInputStream(file);
                    Deployment microServiceDeployment = null;
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    try {
                        microServiceDeployment = mapper.readValue(targetStream, Deployment.class);
                        microServiceDeployment.getMetadata().setNamespace(namespace);
                    } catch (Exception e) {
                        logger.error("Exception: " , e);
                        List deploymentList = client.load(targetStream).inNamespace(namespace).get();
                        microServiceDeployment = (Deployment) deploymentList.get(0);
                    }
                    List<Container> containers = microServiceDeployment.getSpec().getTemplate().getSpec().getContainers();

                    for (Container container : containers) {
                        if (container.getName().contains(Constants.microServiceName)) {
                            container.setImage(microServiceImageName + ":" + utilityService.getVersion(imagesVersionMap.get(microServiceImageName), microServiceImageName, versionNumberString));
                        }
                        enableNewRelic(microServiceEnvironmentNew,container);

                    }
//                    microServiceDeployment.getSpec().setReplicas(1);
                    //microServiceDeployment.getSpec().getSelector().getMatchLabels().put(AFFINITY_LABEL, Constants.microServiceName + "-" + microServiceEnvironmentNew.getCode());
                    microServiceDeployment.getSpec().getSelector().getMatchLabels().remove(AFFINITY_LABEL);
                    microServiceDeployment.getSpec().getTemplate().getMetadata().getLabels().put(AFFINITY_LABEL, Constants.microServiceName + "-" + microServiceEnvironmentNew.getCode());
                    applyAffinity(microServiceEnvironmentNew,microServiceDeployment);


                    CountDownLatch closeLatch = new CountDownLatch(1);

                    //perform rolling update
                    logger.info("namespace : " + namespace + " --> " + "Update started for " + deployment + " --> old version : " + microServiceEnvironmentOld.getVersion() + " new version : " + microServiceEnvironmentNew.getVersion());

                    if(hasEnvironmentVariablesChanged && utilityService.compareVersion(versionOld,versionNew)==0 && (microServiceEnvironmentNew.getEnableNewRelic()==microServiceEnvironmentOld.getEnableNewRelic())&&( (microServiceEnvironmentNew.getNodeAffinity()==microServiceEnvironmentOld.getNodeAffinity()))){
                        // since version has not changed,and the new Relic is not enabled ,only restart of pods are required with fresh environment variables
                        String patch = " kubectl patch deployment "+ deployment+" --namespace="+namespace+" -p   \"{\\\"spec\\\":{\\\"template\\\":{\\\"metadata\\\":{\\\"annotations\\\":{\\\"date\\\":\\\"`date +'%s'`\\\"}}}}}\"";
                        logger.info("Applying Patch");
                        utilityService.executeCommands(patch,true,1);
                    }else {

                        List<EnvVar> envVars = new ArrayList<>();
                        Set<String> keySet = newConfig.getData().keySet();
                        for (String envVariableName : keySet) {
                            ConfigMapKeySelector configMapKeySelector = new ConfigMapKeySelector();
                            configMapKeySelector.setKey(envVariableName);
                            configMapKeySelector.setName(oldConfig.getMetadata().getName());
                            EnvVarSource envVarSource = new EnvVarSource();
                            envVarSource.setConfigMapKeyRef(configMapKeySelector);
                            EnvVar envVar = new EnvVar();
                            envVar.setName(envVariableName);
                            envVar.setValueFrom(envVarSource);
                            envVars.add(envVar);
                        }


                        for (Container container : containers) {
                            if (container.getName().contains(deployment)) {
                                container.getEnv().addAll(envVars);
                            }
                        }

                        microServiceDeployment.getMetadata().setName(deployment);

                        if(!microServiceEnvironmentNew.getAutoscale()) {
                            microServiceDeployment.getSpec().setReplicas(microServiceEnvironmentNew.getTargetPods());
                        }else{
                            microServiceDeployment.getSpec().setReplicas(2);
                        }

                        client.extensions().deployments().inNamespace(namespace).withName(microServiceDeployment.getMetadata().getName()).replace(microServiceDeployment);

                    }
                    Thread.sleep(10000);

                    try (Watch watch = client.extensions().deployments().inNamespace(namespace).watch(new Watcher<Deployment>() {
                        Boolean check = false;

                        @Override
                        public void eventReceived(Action action, Deployment resource) {

                            logger.info("Event received");
                            logger.info("namespace : " + namespace + " --> " + "{} : {} : {}", action, resource.getMetadata().getName(), Constants.microServiceName);

                            logger.info("name " + resource.getMetadata().getName());
                            logger.info("ready " + resource.getStatus().getReadyReplicas());
                            logger.info("unavailable " + resource.getStatus().getUnavailableReplicas());
                            logger.info("updated " + resource.getStatus().getUpdatedReplicas());
                            logger.info("replicas " + resource.getStatus().getReplicas());

                            if (resource.getMetadata().getName().contains(deployment) && resource.getStatus().getReadyReplicas()!=null && resource.getStatus().getReplicas()!=null && resource.getStatus().getReadyReplicas().equals(resource.getStatus().getReplicas())) {

                                closeLatch.countDown();
                                logger.info("namespace : " + namespace + " --> " + "Close Latch updated for " +deployment+" Value : " + closeLatch.getCount());
                            }
                        }

                        @Override
                        public void onClose(KubernetesClientException e) {
                            if (e != null) {
                                logger.error("Exception ", e);
                                while (closeLatch.getCount() != 0) {
                                    closeLatch.countDown();
                                }
                                throw e;
                            }
                        }
                    })) {
                        logger.info("inside try");
                        closeLatch.await(2, TimeUnit.MINUTES);
                        logger.info("inside try after close latch");

                        utilityService.executeCommands(" kubectl rollout status deployment/" + deployment + " --namespace=" + namespace, true, 1);
                    }

                } catch(ScriptException e){
                    //Doesnot require any rollbacks
                    logger.error("Exception: " , e);
                    microServiceEnvironmentOld.setStatus(Constants.updateFailed);
                    microServiceEnvironmentOld.setLogs(e.getMessage());
                    error = true;

                    revertToOldConfig(client,oldConfig,namespace,versionOld, clstr);

                }

                catch (Exception e) {

                    logger.error("Exception: " , e);
                    revertToOldConfig(client,oldConfig,namespace,versionOld, clstr);


                    logger.info("rolling back "+deployment);

                    utilityService.executeCommands("kubectl rollout undo deployment/" +deployment + " --namespace=" + namespace, true, 1);


                    final CountDownLatch closeLatch = new CountDownLatch(1);
                    Thread.sleep(2000);

                    try (Watch watch = client.extensions().deployments().inNamespace(namespace).watch(new Watcher<Deployment>() {
                        Boolean check = false;

                        @Override
                        public void eventReceived(Action action, Deployment resource) {

                            logger.info("Event received");
                            logger.info("namespace : " + namespace + " --> " + "{} : {} : {}", action, resource.getMetadata().getName(), Constants.microServiceName);

                            logger.info("name " + resource.getMetadata().getName());
                            logger.info("ready " + resource.getStatus().getReadyReplicas());
                            logger.info("unavailable " + resource.getStatus().getUnavailableReplicas());
                            logger.info("updated " + resource.getStatus().getUpdatedReplicas());
                            logger.info("replicas " + resource.getStatus().getReplicas());

                            if (resource.getMetadata().getName().contains(deployment) &&  resource.getStatus().getReadyReplicas()!=null && resource.getStatus().getReplicas()!=null && resource.getStatus().getReadyReplicas().equals(resource.getStatus().getReplicas())) {

                                logger.info("namespace : " + namespace + " --> " + Constants.microServiceName+": " + closeLatch.getCount());
                                closeLatch.countDown();
                            }
                        }

                        @Override
                        public void onClose(KubernetesClientException e) {
                            if (e != null) {
                                logger.error("Exception ", e);
                                while (closeLatch.getCount() != 0) {
                                    closeLatch.countDown();
                                }
                                throw e;
                            }
                        }
                    })) {
                        closeLatch.await(5, TimeUnit.MINUTES);
                    }
                    microServiceEnvironmentOld.setStatus(Constants.updateFailed);
                    microServiceEnvironmentOld.setLogs(e.getMessage());
                    error = true;
                }
            }
            if(!error) {

                if ((microServiceEnvironmentOld.getAutoscale() && microServiceEnvironmentNew.getAutoscale()) || (!microServiceEnvironmentOld.getAutoscale() && microServiceEnvironmentNew.getAutoscale())) {

                    if (microServiceEnvironmentOld.getAutoscale()) {
                        client.autoscaling().horizontalPodAutoscalers().inNamespace(namespace).withName(Constants.microServiceHpa).delete();
                    }
                    createHpa(microServiceEnvironmentNew, namespace, client,Constants.microServiceName, Constants.microServiceHpa);

                } else if (microServiceEnvironmentOld.getAutoscale() && !microServiceEnvironmentNew.getAutoscale()) {

                    client.autoscaling().horizontalPodAutoscalers().inNamespace(namespace).withName(Constants.microServiceHpa).delete();
                    client.extensions().deployments().inNamespace(namespace).withName(Constants.microServiceName).scale(microServiceEnvironmentNew.getTargetPods(), true);

                } else if (!microServiceEnvironmentOld.getAutoscale() && !microServiceEnvironmentNew.getAutoscale()) {

                    if (!microServiceEnvironmentNew.getTargetPods().equals(microServiceEnvironmentOld.getTargetPods())) {
                        client.extensions().deployments().inNamespace(namespace).withName(Constants.microServiceName).scale(microServiceEnvironmentNew.getTargetPods(), true);
                    }
                }
                microServiceEnvironmentNew.setStatus(Constants.success);
                microServiceEnvironmentNew.setLogs("");
                logger.info("namespace : " + namespace + " --> " + "Update completed");
            }

        } catch (Exception e1) {
            e1.printStackTrace();
            microServiceEnvironmentOld.setStatus(Constants.updateFailed);
            microServiceEnvironmentOld.setLogs(e1.getMessage());
            error = true;

        }

        if (error) {
            logger.error("Error occurred, updating failed");
            microServiceEnvironmentOld.setLastModifiedDate(DateTime.now());
            microServiceEnvironmentNew = microServiceEnvironmentRepository.save(microServiceEnvironmentOld);
        } else {
            logger.error("update successful");
            microServiceEnvironmentNew.setLastModifiedDate(DateTime.now());
            microServiceEnvironmentNew = microServiceEnvironmentRepository.save(microServiceEnvironmentNew);
        }
        utilityService.executeCommands("rm -rf "+MICROSERVICE_YAML_FOLDER_PATH+"/"+"microService_"+microServiceEnvironmentOld.getMicroService().getCode()+"/"+microServiceEnvironmentNew.getVersion(),true,1);

        return microServiceEnvironmentNew.getStatus();

    }


    private void revertToOldConfig(KubernetesClient client, ConfigMap oldConfig , String namespace, String version, co.fareye.microservicemanager.clusterManager.domain.Cluster clstr) throws IOException, InterruptedException, ScriptException {

        versionToDeploy = version+"/";
        loadConfigFiles(namespace,client,true, clstr);

        if(oldConfig==null){
            return;
        }
        ConfigMap configMap = new ConfigMapBuilder().withNewMetadata().withName(oldConfig.getMetadata().getName()).withNamespace(namespace).endMetadata().withApiVersion(oldConfig.getApiVersion()).withData(oldConfig.getData()).build();
        client.configMaps().inNamespace(namespace).withName(configMap.getMetadata().getName()).replace(configMap);

    }


    private void createHpa(MicroServiceEnvironment microServiceEnvironment, String namespace, KubernetesClient client, String deploymentName, String hpaName) throws FileNotFoundException {
        File file = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"hpa.yaml");

        logger.info("namespace : " + namespace+" --> "+"applying horizontal pod auto-scaler");
        InputStream targetStream = new FileInputStream(file);

        List hpaList  = client.load(targetStream).inNamespace(namespace).get();
        HorizontalPodAutoscaler horizontalPodAutoscaler = (HorizontalPodAutoscaler) hpaList.get(0);
        horizontalPodAutoscaler.getSpec().setMaxReplicas(microServiceEnvironment.getMaximumPods());
        horizontalPodAutoscaler.getSpec().setMinReplicas(microServiceEnvironment.getMinimumPods());
        horizontalPodAutoscaler.getSpec().setTargetCPUUtilizationPercentage(microServiceEnvironment.getCpuUtilization());
        horizontalPodAutoscaler.getMetadata().setName(hpaName);
        horizontalPodAutoscaler.getSpec().getScaleTargetRef().setName(deploymentName);
        client.autoscaling().horizontalPodAutoscalers().createOrReplace(horizontalPodAutoscaler);

        logger.info("namespace : " + namespace+" --> "+"horizontal pod auto-scaler applied");

    }


    public ResponseEntity<Page<MicroServiceEnvironment>> getAllMicroServicesEnvironmentPageable(PageDetailsDTO pageDetailsDTO, String query, Long microServiceId) throws IOException {

        Page<MicroServiceEnvironment> page = null;
        Sort.Direction sortType = Sort.Direction.ASC;
        if (pageDetailsDTO.getSortType().equals("ASC")) {
            sortType = Sort.Direction.ASC;
        } else if (pageDetailsDTO.getSortType().equals("DESC")) {
            sortType = Sort.Direction.DESC;
        }
        Sort sort = new Sort(new Sort.Order(sortType, pageDetailsDTO.getSortOn()));
        PageRequest request = new PageRequest(pageDetailsDTO.getPageNo(), pageDetailsDTO.getRecordsPerPage(), sort);


            if (!query.equals("")) {
                page = microServiceEnvironmentRepository.getListWithQuery(("%" + query + "%").toLowerCase(),microServiceId, request);
            } else {
                page = microServiceEnvironmentRepository.getList(microServiceId, request);

            }
            return new ResponseEntity<>(page, HttpStatus.OK);

    }

    Map<String,String> getEnvironmentVariablesMap(String environmentVariables) {
        Map<String,String> environmentVariableMap = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            environmentVariableMap = objectMapper.readValue(environmentVariables, new TypeReference<Map<String,String>>() {
            });
        } catch (Exception e) {
            logger.error("parse exception");
        }
        return environmentVariableMap;
    }

    public MicroServiceEnvironment setEnvironmentVariables(MicroServiceEnvironment microServiceEnvironment) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String environmentVariableString  = objectMapper.writeValueAsString(microServiceEnvironment.getEnvironmentVariablesMap());
        microServiceEnvironment.setEnvironmentVariables(environmentVariableString);
        return microServiceEnvironment;
    }

    public List<String> checkEnvironmentVariables(String environmentVariables,String version) throws IOException, InterruptedException {
        //TODO add more checks

        List<String> errorList = new ArrayList<>();
        Map<String,String> environmentVariablesMap = getEnvironmentVariablesMap(environmentVariables);
        List<String> envVariableList = getUserDefinedEnvironmentVariableList(version);
        Set<String> defaultEnvironmentVariablesKey = getDefaultEnvironmentVariableList(version);
        if(envVariableList==null || environmentVariables==null){
            errorList.add("Could not find environment variables list");
            return errorList;
        }
        for(Map.Entry<String,String> entry : environmentVariablesMap.entrySet()){
            if(envVariableList.contains(entry.getKey()) && entry.getValue()!=null && !entry.getValue().trim().equalsIgnoreCase("") ){
                envVariableList.remove(entry.getKey());
            }
            if(entry.getValue()!=null && entry.getValue().equalsIgnoreCase("default")){
                if(!defaultEnvironmentVariablesKey.contains(entry.getKey())){
                    errorList.add(entry.getKey()+" key doesn't have any default value specified");
                }
            }
        }
        if(envVariableList.size()>0){
            for( String key : envVariableList){
                errorList.add("Required environment variable "+ key+" not present!");
            }

        }
        return errorList;
    }

    public List<String> getUserDefinedEnvironmentVariableList(String version) throws InterruptedException, IOException {

        String fileName = "/environment-variables-list-for-user.txt";
        File downloadedFile =  s3DownloadService.downloadFile(version+fileName,"/tmp/"+version+fileName,s3UrlService.getS3MicroServiceBucketName());
        List<String> environmentVariableList = utilityService.getListOfFiles("/tmp/"+version+fileName,true);
        downloadedFile.delete();
        return environmentVariableList;
    }

    public Set<String> getDefaultEnvironmentVariableList(String version) throws InterruptedException, IOException {

        String fileName = "/environment-variables.yaml";
        File downloadedFile =  s3DownloadService.downloadFile(version+fileName,"/tmp/"+version+fileName,s3UrlService.getS3MicroServiceBucketName());

        File file = new File("/tmp/"+version+fileName);
        InputStream targetStream = new FileInputStream(file);
        ConfigMap configMap = null;

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            configMap = mapper.readValue(targetStream, ConfigMap.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Exception: " , e);
        }
        downloadedFile.delete();

        return configMap.getData().keySet();
    }

    /**
     * If environment variables has changed from old version, then this function applies the new configuration and returns true.
     * Otherwise it returns false and does nothing.
     *
     * Note: This function assumes that default values doesn't change in version changes
     *
     * @param microServiceEnvironmentNew
     * @param microServiceEnvironmentOld
     * @param client
     * @return
     * @throws FileNotFoundException
     */
    private Map<String, ConfigMap> checkChangeInEnvironmentVariables(MicroServiceEnvironment microServiceEnvironmentNew , MicroServiceEnvironment microServiceEnvironmentOld, KubernetesClient client) throws FileNotFoundException {
        String namespace = microServiceEnvironmentOld.getCode();

        Boolean hasEnvironmentVariablesChanged = false;
        File file = new File(microServiceYamlFolderPathWithMicroServiceCode+versionToDeploy+"environment-variables.yaml");

        logger.info("namespace : " + namespace+" --> "+"checking change in environment variables");
        InputStream targetStream = new FileInputStream(file);

        List configListNew  = client.load(targetStream).inNamespace(namespace).get();
        ConfigMap configNew = (ConfigMap) configListNew.get(0);
        ConfigMap configOld = client.configMaps().inNamespace(namespace).withName(configNew.getMetadata().getName()).get();
        Map<String,String> backupOldConfig = new HashMap<>(configOld.getData());

        for(Map.Entry<String,String> entry : configNew.getData().entrySet()){
            if(!configOld.getData().containsKey(entry.getKey())){
                configOld.getData().put(entry.getKey(),entry.getValue());
                hasEnvironmentVariablesChanged = true;
            }
        }

        Map<String,String> environmentVariableMap = getEnvironmentVariablesMap(microServiceEnvironmentNew.getEnvironmentVariables());
        if(environmentVariableMap!=null) {
            for (Map.Entry<String,String> entry :  environmentVariableMap.entrySet()){
                if(!entry.getValue().equalsIgnoreCase("default")){
                    if(!entry.getValue().equals(configOld.getData().get(entry.getKey()))) {
                        configOld.getData().put(entry.getKey(), entry.getValue());
                        hasEnvironmentVariablesChanged = true;
                    }

                }else{

                    // New Default value
                    if(!configNew.getData().get(entry.getKey()).equals(configOld.getData().get(entry.getKey()))) {
                        configOld.getData().put(entry.getKey(), configNew.getData().get(entry.getKey()));
                        hasEnvironmentVariablesChanged = true;
                    }
                }
            }
        }
        Map<String,ConfigMap> configMaps = new LinkedHashMap<>();

        if(hasEnvironmentVariablesChanged) {

            configNew.setData(configOld.getData());
            client.configMaps().inNamespace(namespace).withName(configOld.getMetadata().getName()).replace(configNew);
            configOld.setData(backupOldConfig);

            configMaps.put("new",configNew);
            configMaps.put("old",configOld);
            logger.info("namespace : " + namespace + " --> " + "environment variables changed and loaded");
            return configMaps;
        }else{
            logger.info("namespace : " + namespace + " --> " + "environment variables have not changed");
            configOld.setData(backupOldConfig);
            configMaps.put("new",null);
            configMaps.put("old",configOld);
            return configMaps;
        }

    }

    public MicroService getMicroServiceByCode(String code) {

        MicroService microService = microServiceManagerRepository.getByCode(code);
        return microService;
    }
    public MicroService getMicroServiceById(Long microServiceId) {

        MicroService microService = microServiceManagerRepository.getById(microServiceId);
        return microService;
    }


    public List<MicroServiceEnvironment> getAllMicroServiceEnvironments() {
        return microServiceEnvironmentRepository.getAllMicroServiceEnvironments();
    }

    public List<MicroServiceEnvironmentDto> getAllMicroServiceEnvironmentsDto() {
        List<MicroServiceEnvironmentDto> microServiceEnvironmentDtos =  new ArrayList<>();

        List<MicroServiceEnvironment> microServiceEnvironments = getAllMicroServiceEnvironments();

        for(MicroServiceEnvironment microServiceEnvironment:microServiceEnvironments){
            MicroServiceEnvironmentDto microServiceEnvironmentDto =  new MicroServiceEnvironmentDto();
            microServiceEnvironmentDto.setId(microServiceEnvironment.getId());
            microServiceEnvironmentDto.setCode(microServiceEnvironment.getCode());
            microServiceEnvironmentDto.setName(microServiceEnvironment.getName());
            microServiceEnvironmentDto.setMicroServiceId(microServiceEnvironment.getMicroService().getId());
            microServiceEnvironmentDtos.add(microServiceEnvironmentDto);
        }
        return microServiceEnvironmentDtos;
    }

    public List<MicroServiceEnvironment> getMicroServiceEnvironmentByMicroServiceId(Long microServiceId) {
        return microServiceEnvironmentRepository.getMicroServiceEnvironmentByMicroServiceId(microServiceId);
    }

    public MicroService saveMicroService(MicroServiceDto microServiceDto) throws Exception {

        MicroService microService = new MicroService(microServiceDto);
        microService.setCreationDate(DateTime.now());
        microService.setNumberOfEnvironments(0);
        microService.setId(0L);

        MicroService oldMicroService = microServiceManagerRepository.getByCode(microService.getCode());
        if(oldMicroService != null){
            throw new Exception("MicroService with given code already exists!");
        }
        Boolean created = true;
        if(microServiceDto.getRegister()) {
            created = createPipelineInCodeship(microService);
        }
        if(created) {
            return microServiceManagerRepository.save(microService);
        }else {
            return null;
        }
    }

    private Boolean createPipelineInCodeship(MicroService microService) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        List list = new ArrayList();
        list.add(MediaType.APPLICATION_JSON);
        headers.setAccept(list);
        String plainCreds = CODESHIP_USERNAME+":"+CODESHIP_PASSWORD;
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);

        headers.add("Authorization", "Basic " + base64Creds);
        RestTemplate restTemplate =  new RestTemplate();
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> authResponse = restTemplate.exchange("https://api.codeship.com/v2/auth", HttpMethod.POST, entity, String.class);
            if (authResponse.getStatusCode().equals(HttpStatus.OK)) {
                JSONObject jsonObject = new JSONObject(authResponse.getBody());
                String accessToken = jsonObject.get("access_token").toString();
                JSONArray organisationArray = (JSONArray) jsonObject.get("organizations");
                String orgId = "";
                Boolean companyFound = false;
                for(int x=0 ;x<organisationArray.length();x++) {
                    jsonObject = organisationArray.getJSONObject(x);
                    orgId = jsonObject.get("uuid").toString();
                    String name = jsonObject.get("name").toString();
                    if(name.equalsIgnoreCase("fareye")){
                        companyFound = true;
                        break;
                    }
                }
                if(!companyFound){
                    logger.info("Company not found in codeship");
                    return false;
                }
                Map<String,String> header = new LinkedHashMap<>();
                header.put("content-type", "application/json");
                header.put("Authorization", "Bearer " + accessToken);
                String googleCredentials = utilityService.getListOfFiles(s3UrlService.getGoogleAccessKeyFilePath(),false).get(0);
                googleCredentials = googleCredentials.replace("\\n","\\\\n");
                googleCredentials = googleCredentials.replace("\"","\\\"");
                String body = "{\"repository_url\":\""+microService.getGitUrl()+"\",\"type\":\"basic\",\"team_ids\":[28937],\"notification_rules\":[{\"notifier\":\"github\",\"branch\":null,\"branch_match\":\"exact\",\"build_statuses\":[\"failed\",\"recovered\",\"started\",\"success\"],\"target\":\"all\",\"options\":{}},{\"notifier\":\"email\",\"branch\":null,\"branch_match\":\"exact\",\"build_statuses\":[\"failed\",\"recovered\"],\"target\":\"all\",\"options\":{}}],\"setup_commands\":[\"export CLOUDSDK_PYTHON_SITEPACKAGES=1\",\"export CLOUDSDK_CORE_DISABLE_PROMPTS=1\",\"curl https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-250.0.0-linux-x86_64.tar.gz > google-cloud-sdk-250.0.0-linux-x86_64.tar.gz\",\"tar -xvzf google-cloud-sdk-250.0.0-linux-x86_64.tar.gz\",\"google-cloud-sdk/install.sh\",\". google-cloud-sdk/path.bash.inc\",\"pip install awscli\"],\"deployment_pipelines\":[],\"environment_variables\":[{\"name\":\"GOOGLE_AUTH_JSON\",\"value\":\""+googleCredentials+"\"}],\"test_pipelines\":[{\"name\":\"Test Commands\",\"commands\":[\"jdk_switcher home oraclejdk8\",\"# /usr/lib/jvm/java-8-oracle\",\"jdk_switcher use oraclejdk8\",\"mvn -Pprod clean package \",\"echo $GOOGLE_AUTH_JSON > credentials.json\",\"gcloud auth activate-service-account --key-file=credentials.json\",\"export AWS_DEFAULT_REGION=us-west-2\",\"export AWS_SECRET_ACCESS_KEY="+s3UrlService.getS3SecretKey()+"\",\"export AWS_ACCESS_KEY_ID="+s3UrlService.getS3AccessKey()+"\",\"gcloud config set project "+MICROSERVICE_PROJECT_ID+"\",\"ls ~/clone\",\"cp ~/clone/target/*.jar ~/clone/Docker/\",\"touch sc\",\"if [ -f ${HOME}/clone/.git/shallow ]; then  git fetch --unshallow; fi\",\"git fetch -t\",\"echo 'version=$(git describe --exact-match --tags --always 2>&1 )' > sc\",\"echo 'result=$?' >> sc\",\"echo 'echo $result' >> sc\",\"echo 'echo $version' >> sc\",\"echo 'if [ $result -eq 0 ]; then' >> sc\",\"echo 'echo \\\"gcloud command is running\\\"'>> sc\",\"echo 'gcloud builds submit --tag asia.gcr.io/"+MICROSERVICE_PROJECT_ID+"/microService_"+microService.getCode()+":$version ~/clone/Docker/' >> sc\",\"echo 'result=$?' >> sc\",\"echo 'if [ $result -ne 0 ]; then' >> sc\",\"echo 'exit $result' >> sc\",\"echo 'fi' >> sc\",\"echo 'echo \\\"AWS command is running\\\"'>> sc\",\"echo 'aws s3 sync ~/clone/yaml s3://"+s3UrlService.getS3MicroServiceBucketName()+"/microService_"+microService.getCode()+"/$version' >> sc\",\"echo 'if [ $result -ne 0 ]; then' >> sc\",\"echo 'exit $result' >> sc\",\"echo 'fi' >> sc\",\"echo 'fi' >> sc \",\"cat sc\",\"bash ./sc\"]}]}";
                HttpResponse<String> response = Unirest.post("https://api.codeship.com/v2/organizations/"+orgId+"/projects")
                        .headers(header)
                        .body(body)
                        .asString();

                if (response.getStatus() == HttpStatus.CREATED.value()) {
                    return true;
                }
                throw new Exception(response.getBody().toString());

            } else {
                logger.info("Codeship authentication error");
                return false;
            }
        }
        catch (Exception e){
            logger.error("Codeship error: ", e);
            throw e;
        }

    }

    public List<String> listOfVersions(String code) {
        return utilityService.sortVersionsDescending(s3DownloadService.listOfVersions(s3UrlService.getS3MicroServiceBucketName(),code));
    }

    public List<String> getDeploymentStepsFromS3(String code,String version1, String version2 ) throws Exception {
        return utilityService.getDeploymentStepsFromS3(code,version1,version2, s3UrlService.getS3MicroServiceBucketName());
    }



    private void applyAffinity (MicroServiceEnvironment microServiceEnvironment, Deployment deployment){

        NodeAffinity node = new NodeAffinity();
        Affinity aff = new Affinity();

        switch(microServiceEnvironment.getNodeAffinity()){
            case "true":
            {
                node = new NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        (NodeSelector) new NodeSelectorBuilder().withNodeSelectorTerms(
                                (NodeSelectorTerm) new NodeSelectorTermBuilder().withMatchExpressions(
                                        (NodeSelectorRequirement) new NodeSelectorRequirementBuilder()
                                                .withKey("node-type")
                                                .withOperator("In")
                                                .withValues("dedicated")
                                                .build()
                                ).build()
                        ).build())
                        .build();
                aff.setNodeAffinity(node);
                deployment.getSpec().getTemplate().getSpec().setAffinity(aff);
                break;
            }
            case "false":
            {
                node = new NodeAffinityBuilder().withRequiredDuringSchedulingIgnoredDuringExecution(
                        (NodeSelector) new NodeSelectorBuilder().withNodeSelectorTerms(
                                (NodeSelectorTerm) new NodeSelectorTermBuilder().withMatchExpressions(
                                        (NodeSelectorRequirement) new NodeSelectorRequirementBuilder()
                                                .withKey("node-type")
                                                .withOperator("NotIn")
                                                .withValues("dedicated")
                                                .build()
                                ).build()
                        ).build())
                        .build();

                String podLabelValue =deployment.getSpec().getTemplate().getMetadata().getLabels().get(AFFINITY_LABEL);

                PodAntiAffinity podSelfAntiAffinity=new PodAntiAffinityBuilder().addToRequiredDuringSchedulingIgnoredDuringExecution(
                        ((PodAffinityTerm) new PodAffinityTermBuilder().withLabelSelector((
                                new LabelSelectorBuilder()
                                        .addToMatchExpressions(
                                                (LabelSelectorRequirement) ((new LabelSelectorRequirementBuilder()
                                                        .withKey(AFFINITY_LABEL)
                                                        .withOperator("In")
                                                        .withValues(podLabelValue).build()))
                                        )).build()).withTopologyKey("kubernetes.io/hostname")
                                .build())).build();

                aff.setPodAntiAffinity(podSelfAntiAffinity);
                aff.setNodeAffinity(node);
                deployment.getSpec().getTemplate().getSpec().setAffinity(aff);

                break;
            }
            case "none":{

                deployment.getSpec().getTemplate().getSpec().setAffinity(null);

            }
        }
        return;
    }


    private void enableNewRelic (MicroServiceEnvironment microServiceEnvironment,Container container){

        if(microServiceEnvironment.getEnableNewRelic()){
            container.getCommand().add(1,"-javaagent:/app/newrelic/newrelic.jar");
        }
        else{
            container.getCommand().remove("-javaagent:/app/newrelic/newrelic.jar");

        }

    }

}
