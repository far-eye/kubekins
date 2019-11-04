package co.fareye.microservicemanager.clusterManager.domain;

import co.fareye.microservicemanager.clusterManager.dto.ClusterDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "cluster")
@JsonInclude(JsonInclude.Include.ALWAYS)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Cluster implements Serializable {


    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 0L;


    @Column(name = "project_id")
    private String projectid;

    @Column(name = "region")
    private String region;

    @NotNull
    @Column(name = "cluster_name")
    private String clustername;

    @Column(name = "cluster_description")
    private String clusterDescription;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "created_by")
    private String createdBy = "System";

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    @Column(name = "created_date")
    private DateTime createdDate = DateTime.now();

    public Cluster() {
    }

    public Cluster(ClusterDTO clusterDTO) {
        this.id = clusterDTO.getId();
        this.projectid = clusterDTO.getProjectid();
        this.clustername = clusterDTO.getClustername();
        this.region = clusterDTO.getRegion();
        this.clusterDescription = clusterDTO.getClusterDescription();
        this.domainName = clusterDTO.getDomainName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectid() {
        return projectid;
    }

    public void setProjectid(String projectid) {
        this.projectid = projectid;
    }

    public String getClustername() {
        return clustername;
    }

    public void setClustername(String clustername) {
        this.clustername = clustername;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getClusterDescription() {
        return clusterDescription;
    }

    public void setClusterDescription(String clusterDescription) {
        this.clusterDescription = clusterDescription;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id: '" + id + "\'" +
                ", projectid: '" + projectid + "\'" +
                ", region: '" + region + "\'" +
                ", clustername: '" + clustername + "\'" +
                ", clusterDescription: '" + clusterDescription + "\'" +
                ", createdDate: '" + createdDate + "\'" +
                ", domainName: '" + domainName + "\'" +
                "}";
    }
}
