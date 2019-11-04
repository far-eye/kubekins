package co.fareye.microservicemanager.clusterManager.service;

import co.fareye.microservicemanager.clusterManager.domain.Cluster;
import co.fareye.microservicemanager.clusterManager.dto.ClusterDTO;
import co.fareye.microservicemanager.clusterManager.repository.ClusterManagerRepository;
import co.fareye.microservicemanager.config.GCloudRegions;
import co.fareye.microservicemanager.core.repository.MicroServiceEnvironmentRepository;
import co.fareye.microservicemanager.core.service.S3DownloadService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClusterManagerService {

    @Inject
    private ClusterManagerRepository clusterManagerRepository;

@Inject
private MicroServiceEnvironmentRepository microServiceEnvironmentRepository;
    @Inject
    private S3DownloadService awsConfiguration;

    @Value("${HOME_PATH}")
    private String HOME_PATH;


    public Cluster saveCluster(ClusterDTO clusterDto) throws Exception {

        Cluster cluster = new Cluster(clusterDto);
        cluster.setCreatedDate(DateTime.now());
        cluster.setId(0L);
        Cluster oldCluster = clusterManagerRepository.getByClusterName(cluster.getClustername());
        if (oldCluster != null) {
            throw new Exception("Cluster with given name already exists!");
        }
        return clusterManagerRepository.save(cluster);
    }


    public int deleteCluster(Long id) throws Exception {
        int noOfRows = 0;
        Cluster oldCluster = clusterManagerRepository.getByClusterId(id);
        if (oldCluster == null) {
            throw new Exception("Cluster with given name does not exists!");
        } else {
            if (!isClusterInUse(oldCluster.getId())) {
                noOfRows = clusterManagerRepository.deleteCluster(id);
            } else {
                throw new Exception("Cluster with given name is in use!");
            }
        }
        return noOfRows;
    }


    public int updateCluster(ClusterDTO clusterDto) throws Exception {
        int noOfRows = 0;
        Cluster cluster = new Cluster(clusterDto);
        Cluster oldCluster = clusterManagerRepository.getByClusterId(cluster.getId());
        if (oldCluster == null) {
            throw new Exception("Cluster with given name does not exists!");
        } else {
            noOfRows = clusterManagerRepository.updateCluster(cluster.getProjectid(), cluster.getClustername(), cluster.getRegion(), cluster.getClusterDescription(), cluster.getId());
        }
        return noOfRows;
    }

    public List<Cluster> getClusters() throws Exception {
        return clusterManagerRepository.getClusterList();
    }



    public boolean isClusterInUse(Long id) {
        long noOfFE = microServiceEnvironmentRepository.countByClusterId(id);
        if (noOfFE > 0 ) {
            return true;
        }
        return false;
    }


    public String uploadFilesToS3(MultipartFile cert, MultipartFile key, String clusterName) throws IOException, InterruptedException {

        String relativeFilePath = "/cluster/" + clusterName + "/";
        String filePath = createDir(relativeFilePath);//To create /tmp/env/cluster/cluster_name directory
        if (cert != null)
            addFileToTemp(cert, filePath, relativeFilePath);
        if (key != null)
            addFileToTemp(key, filePath, relativeFilePath);
        awsConfiguration.downloadFile();
        return "success";
    }

    private void addFileToTemp(MultipartFile multipartFile, String filePath, String relativeFilePath) throws IOException, InterruptedException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        inputStream = multipartFile.getInputStream();
        File newFile = new File(filePath + "/" + multipartFile.getOriginalFilename());
        if (!newFile.exists()) {
            newFile.createNewFile();
        }
        outputStream = new FileOutputStream(newFile);
        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
        }
        outputStream.close();
        inputStream.close();
        uploadFileToS3(newFile, relativeFilePath);
    }


    public String createDir(String filePath) {
        File file = new File(HOME_PATH + "/tmp" + filePath);
        if (!file.exists()) {
            file.mkdirs();
            if (!file.exists()) {
                System.out.println("Failed to create directory!");
            } else {
                System.out.println("Directory Created.");
            }
        }
        filePath = file.getAbsolutePath();
        return filePath;
    }

    @Async
    public void uploadFileToS3(File file, String filePath) throws InterruptedException {
        awsConfiguration.upload(file, filePath);
    }


    public String deleteFilesFromS3(String clusterName) {
        awsConfiguration.delete("cluster/" + clusterName + "/");
        return "success";
    }


    public Map<String, String> getRegionMap() {
        Map<String, String> regionMap = new LinkedHashMap<>();
        for (GCloudRegions region : GCloudRegions.values()) {
            regionMap.put(region.getCode(), region.getName());
        }
        return regionMap;
    }

}
