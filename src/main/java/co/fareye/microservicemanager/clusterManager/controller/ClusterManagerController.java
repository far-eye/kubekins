package co.fareye.microservicemanager.clusterManager.controller;

import co.fareye.microservicemanager.clusterManager.domain.Cluster;
import co.fareye.microservicemanager.clusterManager.dto.ClusterDTO;
import co.fareye.microservicemanager.clusterManager.service.ClusterManagerService;
import co.fareye.microservicemanager.core.dto.ApiResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app")
public class ClusterManagerController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    ClusterManagerService clusterManagerService;


    @RequestMapping(value = "/rest/cluster",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getClusters() {

        try {
            List<Cluster> clusters = clusterManagerService.getClusters();
            if (clusters == null) {
                return new ResponseEntity<>(new ApiResponseMessage("No Clusters exist."), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(clusters, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Exception occured", HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/rest/cluster",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity saveCluster(@RequestBody ClusterDTO clusterDTO) {

        String errorMessage;
        try {
            Cluster cluster = clusterManagerService.saveCluster(clusterDTO);
            if (cluster == null) {
                errorMessage = "Could not register cluster";
                return new ResponseEntity<>(new ApiResponseMessage(errorMessage), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(cluster, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Exception: ", e);
            return new ResponseEntity<>(new ApiResponseMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/rest/cluster",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteCluster(@RequestParam(value = "id") Long id) {

        String errorMessage;
        try {
            int noOfRows = clusterManagerService.deleteCluster(id);
            if (noOfRows == 0) {
                errorMessage = "Could not update cluster";
                return new ResponseEntity<>(new ApiResponseMessage(errorMessage), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(new ClusterDTO(), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Exception: ", e);
            return new ResponseEntity<>(new ApiResponseMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/rest/cluster",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity modifyCluster(@RequestBody ClusterDTO clusterDTO) {

        String errorMessage;
        try {
            int noOfRows = clusterManagerService.updateCluster(clusterDTO);
            if (noOfRows == 0) {
                errorMessage = "Could not update cluster";
                return new ResponseEntity<>(new ApiResponseMessage(errorMessage), HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(clusterDTO, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Exception: ", e);
            return new ResponseEntity<>(new ApiResponseMessage(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/rest/gcloud_regions",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity region() {

        Map<String, String> regionMap = clusterManagerService.getRegionMap();
        return new ResponseEntity<>(regionMap, HttpStatus.OK);
    }


    @RequestMapping(value = "/rest/upload_file",
            method = RequestMethod.POST, consumes = {"multipart/form-data"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadFileMulti(
            @RequestParam(name="uploadedCert", required = false) MultipartFile cert, @RequestParam(name="uploadedKey", required = false) MultipartFile key, @RequestParam("clustername") String clusterName) {

        try {
            if(cert==null && key==null){
                return new ResponseEntity<>(new ApiResponseMessage("No File to upload"), HttpStatus.OK);
            }
            clusterManagerService.uploadFilesToS3(cert, key, clusterName);
            return new ResponseEntity<>(new ApiResponseMessage("File uploaded Successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponseMessage("File upload Failed"), HttpStatus.FORBIDDEN);
        }
    }


    @RequestMapping(value = "/rest/delete_file",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFiles(@RequestParam(value = "clusterName") String clusterName) {
        try {
            clusterManagerService.deleteFilesFromS3(clusterName);
            return new ResponseEntity<>(new ApiResponseMessage("File deleted Successfully"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ApiResponseMessage("File deletion Failed"), HttpStatus.FORBIDDEN);
        }
    }


}
