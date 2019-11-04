package co.fareye.microservicemanager.core.dto;

import co.fareye.microservicemanager.core.domain.MicroServiceEnvironment;

import java.io.Serializable;

public class MicroServiceRabbitMqDto implements Serializable {

    String event;
    String source;
    MicroServiceEnvironment microServiceEnvironmentNew;
    MicroServiceEnvironment microServiceEnvironmentOld;

    public MicroServiceRabbitMqDto(MicroServiceEnvironment microServiceEnvironmentNew, MicroServiceEnvironment microServiceEnvironmentOld, String event,String source) {
        this.microServiceEnvironmentNew = microServiceEnvironmentNew;
        this.microServiceEnvironmentOld = microServiceEnvironmentOld;
        this.event = event;
        this.source = source;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public MicroServiceRabbitMqDto() {
    }



    public MicroServiceEnvironment getMicroServiceEnvironmentNew() {
        return microServiceEnvironmentNew;
    }

    public void setMicroServiceEnvironmentNew(MicroServiceEnvironment microServiceEnvironmentNew) {
        this.microServiceEnvironmentNew = microServiceEnvironmentNew;
    }

    public MicroServiceEnvironment getMicroServiceEnvironmentOld() {
        return microServiceEnvironmentOld;
    }

    public void setMicroServiceEnvironmentOld(MicroServiceEnvironment microServiceEnvironmentOld) {
        this.microServiceEnvironmentOld = microServiceEnvironmentOld;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "MicroServiceRabbitMqDto{" +
                "event='" + event + '\'' +
                ", source='" + source + '\'' +
                ", microServiceEnvironmentNew=" + microServiceEnvironmentNew +
                ", microServiceEnvironmentOld=" + microServiceEnvironmentOld +
                '}';
    }
}
