package co.fareye.microservicemanager.clusterManager.dto;

public class ClusterFilesDTO {

    public String fileName;
    public String filePath;
    public String lastModifiedTime;

    public ClusterFilesDTO() {
    }

    public ClusterFilesDTO(String fileName, String filePath, String lastModifiedTime) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
