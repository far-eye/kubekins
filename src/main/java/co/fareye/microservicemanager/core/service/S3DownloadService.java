package co.fareye.microservicemanager.core.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3DownloadService {

    private final static Logger logger = LoggerFactory.getLogger(S3DownloadService.class);

    @Inject
    private TransferManager transferManager;

    @Value("${HOME_PATH}")
    private String HOME_PATH;

    @Value("${S3_CLUSTER_BUCKET}")
    private String bucketName;



    /**
     * DOWNLOAD FILE from Amazon S3
     */
    public void downloadFolder(String keyName, String downloadFilePath, String bucketName) throws InterruptedException {

        logger.info("Downloading configuration files from s3");
        com.amazonaws.services.s3.transfer.MultipleFileDownload download = transferManager.downloadDirectory(bucketName,keyName, new File(downloadFilePath));

        try {
            download.waitForCompletion();
            logger.info("Downloading completed!");

        } catch (Exception e) {
            throw e;
        }
    }

    public File downloadFile(String keyName, String downloadFilePath , String bucketName) throws InterruptedException {

        File downloadedFile = new File(downloadFilePath);
        logger.info("Downloading configuration files from s3");
        Download download = transferManager.download(bucketName,keyName, downloadedFile);

        try {
            download.waitForCompletion();
            logger.info("Downloading completed!");
            return downloadedFile;

        } catch (Exception e) {
            throw e;
        }
    }


    public List<String> listOfVersions(String bucketName){

        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withDelimiter("/");
        ListObjectsV2Result listing = transferManager.getAmazonS3Client().listObjectsV2(req);

        List<String> versionList = new ArrayList<>();
        for (String commonPrefix : listing.getCommonPrefixes()) {
            if(commonPrefix.matches("^v[\\d|\\.]+/$")){
                versionList.add(commonPrefix.replace("/",""));
            }
        }

        return versionList;
    }

    public List<String> listOfVersions(String bucketName,String prefix){

        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName).withDelimiter("/").withPrefix(prefix+"/");
        ListObjectsV2Result listing = transferManager.getAmazonS3Client().listObjectsV2(req);
        List<String> versionList = new ArrayList<>();
        for (String commonPrefix : listing.getCommonPrefixes()) {
            commonPrefix = commonPrefix.replace(prefix+"/","");
            if(commonPrefix.matches("^v[\\d|\\.]+/$")){
                versionList.add(commonPrefix.replace("/",""));
            }
        }

        return versionList;
    }
    public S3Object getS3Object(String bucketName,String keyName){

        AmazonS3 amazonS3 = transferManager.getAmazonS3Client();
        S3Object o = amazonS3.getObject(bucketName, keyName);
        return o;
    }


    @PostConstruct //Enable if files need to download from the S3 Bucket on server startup.
    public void downloadFile() throws InterruptedException {
        File downloadedFile = new File(HOME_PATH+"/tmp/");
        logger.info("Downloading configuration files from s3");
        com.amazonaws.services.s3.transfer.MultipleFileDownload download1 = transferManager.downloadDirectory(bucketName, "cluster/", downloadedFile);
        logger.info("Downloaded files " + download1.getBucketName());
        try {
            download1.waitForCompletion();
            logger.info("Downloading completed!");
        } catch (Exception e) {
            throw e;
        }
    }


    public void upload(File file, String path) throws InterruptedException {
        path = path.substring(1);
        logger.info("Uploading configuration files to s3" + file.getAbsolutePath() + "path " + path);
        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, path + file.getName(), file).withStorageClass(StorageClass.ReducedRedundancy);
            request.withCannedAcl(CannedAccessControlList.PublicRead);
            Upload xfer = transferManager.upload(request);
            xfer.waitForUploadResult();
            logger.info("File Upload completed!" + xfer.getState().name());
//            downloadFile();
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage() + " ---------------- ");
            throw e;
        }
    }


    public void delete(String path){
//        path = path.substring(5);
        logger.info("Deleting files from s3" + "path " + path);
        try {
            AmazonS3 s3Client =  transferManager.getAmazonS3Client();
            for (S3ObjectSummary file : s3Client.listObjects(bucketName, path).getObjectSummaries()){
                s3Client.deleteObject(bucketName, file.getKey());
            }
            logger.info("File Deletion completed!");
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage() + " ---------------- ");
            throw e;
        }
    }


}
