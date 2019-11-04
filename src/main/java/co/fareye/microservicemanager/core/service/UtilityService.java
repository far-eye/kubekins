package co.fareye.microservicemanager.core.service;

import co.fareye.microservicemanager.clusterManager.dto.ClusterFilesDTO;
import co.fareye.microservicemanager.config.Exceptions.ScriptException;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
public class UtilityService {

    private final Logger logger = LoggerFactory.getLogger(UtilityService.class);


    @Value("${HOME_PATH}")
    private String HOME_PATH;

    @Inject
    private S3DownloadService s3DownloadService;


    public List<String> getListOfFiles(String fileName,Boolean showLogs)
            throws IOException {
        // Read one text line at a time and display.
        List<String> fileList = new ArrayList<>();
        File file = new File(fileName);

        BufferedReader reader = new BufferedReader(new
                InputStreamReader(new FileInputStream(file)));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;
            fileList.add(line);
            if(showLogs)
                logger.info(line);
        }
        return fileList;
    }


    public List<String> getDeploymentStepsFromS3(String env , String version1, String version2 , String bucketName) throws Exception {

        Boolean versionCheck1 = false;
        Boolean versionCheck2 = false;
        List<String> versionList = s3DownloadService.listOfVersions(bucketName);
        List<String> versionListForWhichDeploymentStepsAreFetched = new ArrayList<>();
        versionList = sortVersions(versionList);
        int resultVersionCheck = compareVersion(version1,version2);
        if( resultVersionCheck == 1){

            String temp = version2;
            version2 = version1;
            version1 = temp;
        }
        int x = 0;
        for (; x < versionList.size() ; x++ ) {
            if (compareVersion(versionList.get(x), version1) == 0) {
                versionListForWhichDeploymentStepsAreFetched.add(version1);
                versionCheck1 = true;
                break;
            }
        }
        String fileName = "/release_notes.txt";
        if(versionCheck1 && resultVersionCheck == 0) {
            // versions are equal and exists

            File downloadedFile =  s3DownloadService.downloadFile(env+version1+fileName,"/tmp/"+version1+fileName,bucketName);
            List<String> deploymentSteps = new ArrayList<>();
//            deploymentSteps.add("<b>"+version1+"</b>");

            deploymentSteps.addAll(getListOfFiles("/tmp/"+version1+fileName,true));
            downloadedFile.delete();
            return deploymentSteps;

        }else if(versionCheck1 && resultVersionCheck != 0){
            // versions are not same and version1 exists
            x++;
            for (;x < versionList.size() ; x++ ) {
                versionListForWhichDeploymentStepsAreFetched.add(versionList.get(x));
                if (compareVersion(versionList.get(x), version2) == 0) {
                    versionCheck2 = true;
                    break;
                }
            }
            if(versionCheck2){
                List<String> deploymentSteps = new ArrayList<>();
                for(String version :  versionListForWhichDeploymentStepsAreFetched){
                    File downloadedFile =  s3DownloadService.downloadFile(env+version+fileName,"/tmp/"+version+fileName,bucketName);
                    List<String> deploymentStepsTemp = getListOfFiles("/tmp/"+version+fileName,true);
                    downloadedFile.delete();
                    if(!deploymentStepsTemp.isEmpty()) {
//                        deploymentSteps.add("<b>"+version+"</b>");
                        deploymentSteps.addAll(deploymentStepsTemp);
                    }
                }
                return deploymentSteps;
            }
        }

        throw new Exception("Versions are not correct");
    }

    /**
     * This method is used to run bash commands from java. Used majorly for using kubectl from application. Timeout is 2 minutes
     * @param command The command to run on bash
     * @param retry if true will try again to run with give number of reattempts count
     * @param reattempts
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws ScriptException
     */

    public String executeCommands(String command , Boolean retry , Integer reattempts) throws IOException, InterruptedException, ScriptException {

        return executeCommands(command,retry,reattempts,2L);
    }

    /**
     * This method is used to run bash commands from java. Used majorly for using kubectl from application
     * @param command The command to run on bash
     * @param retry if true will try again to run with give number of reattempts count
     * @param reattempts
     * @param timeout in minutes to wait for process to finish before killing the process
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws ScriptException
     */

    public String executeCommands(String command , Boolean retry , Integer reattempts,Long timeout) throws IOException, InterruptedException, ScriptException {

        StringBuffer output = new StringBuffer();
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(new File(HOME_PATH));

        pb.redirectErrorStream(true);
        Process process = pb.start();
        if(!process.waitFor(timeout, TimeUnit.MINUTES)) {
            process.destroy();
        }
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while ((line = reader.readLine()) != null) {
            output.append(line + "\n");
        }
        logger.info(output.toString());
        if (process.exitValue() == 0) {
            return output.toString();
        } else {
            if(retry==true && !(reattempts <= 0)){

                reattempts--;
                logger.info("reattempting",Thread.currentThread().getStackTrace());
                Thread.sleep(2000);
                return output+"\n"+executeCommands(command,retry,reattempts);

            }else {
                throw new ScriptException(output.toString());
            }
        }
    }


    // sorts asc
    public List<String> sortVersions(List<String> versions) {

        int n = versions.size();
        for (int j = 1; j < n; j++) {
            String key = versions.get(j);
            int i = j-1;
            while ( (i > -1) && ( compareVersion(versions.get(i) , key)==1 ) ) {
                versions.set(i+1,versions.get(i));
                i--;
            }
            versions.set(i+1,key);
        }
        return versions;
    }

    public List<String> sortVersionsDescending(List<String> versions) {

        int n = versions.size();
        for (int j = 1; j < n; j++) {
            String key = versions.get(j);
            int i = j-1;
            while ( (i > -1) && ( compareVersion(versions.get(i) , key)== -1 ) ) {
                versions.set(i+1,versions.get(i));
                i--;
            }
            versions.set(i+1,key);
        }
        return versions;
    }

    public String getVersion(List<String> versionMap, String component, String versionNumberString){

        logger.info("version selected for "+component+" :" );
        String requiredVersion = versionNumberString;
        String maxVersion = versionMap.get(0);
        for(String ver : versionMap){
            try {
                String currentVersion = ver;

                int retval = compareVersion(currentVersion,requiredVersion);
                if(retval ==0) {
                    logger.info(ver);
                    return ver;
                } else if(retval < 0) {
                    int retval2 = compareVersion(currentVersion,maxVersion);
                    if(retval2>0){
                        maxVersion = currentVersion;
                    }
                }
            }catch (NumberFormatException e){
                logger.info("Exception" , e);
            }

        }
        logger.info(maxVersion);

        return maxVersion;
    }

    /**
     * if versionFirst is greater than versionSecond then function returns 1
     * if versionFirst is lesser than versionSecond then function returns -1
     * if versionFirst is equal to versionSecond then function returns 0
     *
     * @param versionFirst
     * @param versionSecond
     * @return -1,0 or 1
     */
    public int compareVersion(String versionFirst, String versionSecond) {

        versionFirst = versionFirst.replace("v","");
        versionSecond = versionSecond.replace("v","");
        String [] versionFirstArray = versionFirst.split("\\.");
        String [] versionSecondArray = versionSecond.split("\\.");


        if(versionFirstArray.length > versionSecondArray.length){
            int x = 0;
            String[] temp = new String[versionFirstArray.length];
            for( ; x < versionSecondArray.length ; x++){
                temp[x]=versionSecondArray[x];
            }

            for(;x<versionFirstArray.length;x++ ){
                temp[x]="0";
            }
            versionSecondArray = temp;
        }else if(versionFirstArray.length < versionSecondArray.length){
            int x = 0;
            String[] temp = new String[versionSecondArray.length];
            for( ; x < versionFirstArray.length ; x++){
                temp[x]=versionFirstArray[x];
            }

            for(;x<versionSecondArray.length;x++ ){
                temp[x]="0";
            }
            versionFirstArray = temp;
        }

        int result =0;
        for( int x = 0; x < versionFirstArray.length ; x++ ){
            Integer first = Integer.parseInt(versionFirstArray[x]);
            Integer second = Integer.parseInt(versionSecondArray[x]);

            if(first > second){
                return 1;
            }else if(first < second){
                return -1;
            }

        }
        return result;
    }

    public void authenticateWithKubectl(String projectId,String clusterName,String region) throws InterruptedException, ScriptException, IOException {
        List<String> commands = new ArrayList<>();

        commands.add("gcloud config set project "+projectId);
        commands.add("gcloud config set compute/zone "+region);
        commands.add("gcloud config set container/cluster "+clusterName);
        commands.add("export KUBECONFIG="+HOME_PATH+"'/.kube/config' && gcloud container clusters get-credentials "+clusterName);

        for(int x=0 ;x<commands.size();x++){
            executeCommands(commands.get(x),true,1);
        }

    }


    public ClusterFilesDTO getClusterFiles(String directoryName, String type) throws IOException {
        ClusterFilesDTO fileDto=new ClusterFilesDTO();
        File folder = new File(directoryName);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            if (listOfFiles.length > 0) {
                Arrays.sort(listOfFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                for (File file : listOfFiles) {
                    if (file.isFile() && type.equalsIgnoreCase("key")) {
                        if (file.getName().endsWith(".key")) {
                            fileDto=new ClusterFilesDTO(file.getName(), file.getAbsolutePath(),"");
                        }
                    }else if(file.isFile() && type.equalsIgnoreCase("cert")){
                        if (file.getName().endsWith(".cert") || file.getName().endsWith(".crt")) {
                            fileDto=new ClusterFilesDTO(file.getName(), file.getAbsolutePath(),"");
                        }
                    } else if (file.isDirectory()) {
                        getClusterFiles(file.getAbsolutePath(), type);
                    }
                }
            }
        }
        return fileDto;
    }
}
