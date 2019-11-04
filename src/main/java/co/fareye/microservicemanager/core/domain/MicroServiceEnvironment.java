package co.fareye.microservicemanager.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

@Entity
@Table(name = "microService_environment")
public class MicroServiceEnvironment implements Serializable{


    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 0L;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "creation_date")
    private DateTime creationDate;

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "last_modified_date")
    private DateTime lastModifiedDate;


    @Column(name = "status")
    private String status;

    @Column(name = "version")
    private String version;

    @Column(name = "old_version")
    private String oldVersion;


    @Column(name = "logs")
    private String logs;

    @Column(name="cluster_id")
    private Long clusterid;

    @NotNull
    @Column(name = "autoscale")
    private Boolean autoscale = false;

    @Column(name = "target_pods")
    private Integer targetPods = 1;

    @Column(name = "minimum_pods")
    private Integer minimumPods = 1;

    @Column(name = "maximum_pods")
    private Integer maximumPods = 1;

    @Column(name = "cpu_utilization")
    private Integer cpuUtilization = 60;

    // If true manually delete all pvc , pv and backed storage media
    @Column(name = "keep_environment_data_after_deletion")
    private Boolean keepEnvironmentDataAfterDeletion = false;

    @Column(name = "node_affinity")
    private String nodeAffinity;

    @Column(name = "allow_new_relic")
    private Boolean enableNewRelic = false;

    @Column(name = "environment_variables")
    private String environmentVariables;

    @Transient
    private Map<String,String> environmentVariablesMap;


    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="microService_id", columnDefinition="integer")
    private MicroService microService;

    @Transient
    private Long microService_id;


    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getAutoscale() {
        return autoscale;
    }

    public void setAutoscale(Boolean autoscale) {
        this.autoscale = autoscale;
    }

    public Integer getTargetPods() {
        return targetPods;
    }

    public void setTargetPods(Integer targetPods) {
        this.targetPods = targetPods;
    }

    public Integer getMinimumPods() {
        return minimumPods;
    }

    public void setMinimumPods(Integer minimumPods) {
        this.minimumPods = minimumPods;
    }

    public Integer getMaximumPods() {
        return maximumPods;
    }

    public void setMaximumPods(Integer maximumPods) {
        this.maximumPods = maximumPods;
    }

    public Integer getCpuUtilization() {
        return cpuUtilization;
    }

    public void setCpuUtilization(Integer cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public Boolean getKeepEnvironmentDataAfterDeletion() {
        return keepEnvironmentDataAfterDeletion;
    }

    public void setKeepEnvironmentDataAfterDeletion(Boolean keepEnvironmentDataAfterDeletion) {
        this.keepEnvironmentDataAfterDeletion = keepEnvironmentDataAfterDeletion;
    }

    public String getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public Map<String, String> getEnvironmentVariablesMap() {
        return environmentVariablesMap;
    }

    public void setEnvironmentVariablesMap(Map<String, String> environmentVariablesMap) {
        this.environmentVariablesMap = environmentVariablesMap;
    }

    public MicroService getMicroService() {
        return microService;
    }

    public void setMicroService(MicroService microService) {
        this.microService = microService;
    }

    public Long getMicroService_id() {
        return microService_id;
    }

    public void setMicroService_id(Long microService_id) {
        this.microService_id = microService_id;
    }

    public DateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(DateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public void setOldVersion(String oldVersion) {
        this.oldVersion = oldVersion;
    }

    public Long getClusterid() {
        return clusterid;
    }

    public void setClusterid(Long clusterid) {
        this.clusterid = clusterid;
    }

    public Boolean getEnableNewRelic() { return enableNewRelic; }

    public void setEnableNewRelic(Boolean enableNewRelic) { this.enableNewRelic = enableNewRelic; }


    public String getNodeAffinity() { return nodeAffinity;    }

    public void setNodeAffinity(String nodeAffinity) {  this.nodeAffinity = nodeAffinity; }

    @Override
    public String toString() {
        return "MicroServiceEnvironment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", creationDate=" + creationDate +
                ", lastModifiedDate=" + lastModifiedDate +
                ", status='" + status + '\'' +
                ", version='" + version + '\'' +
                ", oldVersion='" + oldVersion + '\'' +
                ", logs='" + logs + '\'' +
                ", autoscale=" + autoscale +
                ", targetPods=" + targetPods +
                ", minimumPods=" + minimumPods +
                ", maximumPods=" + maximumPods +
                ", cpuUtilization=" + cpuUtilization +
                ", keepEnvironmentDataAfterDeletion=" + keepEnvironmentDataAfterDeletion +
                ", environmentVariables='" + environmentVariables + '\'' +
                ", environmentVariablesMap=" + environmentVariablesMap +
                ", microService=" + microService +
                ", microService_id=" + microService_id +
                ", clusterid=" + clusterid +
                ", enableNewRelic=" + enableNewRelic +
                ", nodeAffinity=" + nodeAffinity +
                '}';
    }

}
