package co.fareye.microservicemanager.core.domain;

import co.fareye.microservicemanager.core.dto.MicroServiceDto;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "microService")
public class MicroService implements Serializable {


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

    @Column(name = "git_url")
    private String gitUrl;

    @Column(name = "number_of_environments")
    private Integer numberOfEnvironments = 0;


    @OneToMany(cascade = CascadeType.ALL, mappedBy = "microService", orphanRemoval = true, fetch = FetchType.EAGER)
    List<MicroServiceEnvironment> microServiceEnvironments;

    public MicroService() {
    }

    public MicroService(MicroServiceDto microServiceDto) {

        this.id = microServiceDto.getId();
        this.name = microServiceDto.getName();
        this.code = microServiceDto.getCode();
        this.gitUrl = microServiceDto.getGitUrl();
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

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public List<MicroServiceEnvironment> getMicroServiceEnvironments() {
        return microServiceEnvironments;
    }

    public void setMicroServiceEnvironments(List<MicroServiceEnvironment> microServiceEnvironments) {
        this.microServiceEnvironments = microServiceEnvironments;
    }

    public Integer getNumberOfEnvironments() {
        return numberOfEnvironments;
    }

    public void setNumberOfEnvironments(Integer numberOfEnvironments) {
        this.numberOfEnvironments = numberOfEnvironments;
    }

    @Override
    public String toString() {
        return "MicroService{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", creationDate=" + creationDate +
                ", gitUrl='" + gitUrl + '\'' +
                ", numberOfEnvironments=" + numberOfEnvironments +
                '}';
    }
}
