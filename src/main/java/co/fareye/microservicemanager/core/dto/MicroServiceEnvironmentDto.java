package co.fareye.microservicemanager.core.dto;

import java.io.Serializable;

public class MicroServiceEnvironmentDto implements Serializable {


    private Long id;

    private String name;

    private String code;

    private Long microServiceId;


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

    public Long getMicroServiceId() {
        return microServiceId;
    }

    public void setMicroServiceId(Long microServiceId) {
        this.microServiceId = microServiceId;
    }
}
