package co.fareye.microservicemanager.core.service;

import co.fareye.microservicemanager.config.Constants;
import co.fareye.microservicemanager.core.dto.MicroServiceRabbitMqDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class MicroServiceQueueListener {
    private final Logger log = LoggerFactory.getLogger(MicroServiceQueueListener.class);

    @Inject
    private MicroServiceManager microServiceManager;

    public void microServiceQueueListener(MicroServiceRabbitMqDto microServiceRabbitMqDto) throws Exception {

        if (microServiceRabbitMqDto.getSource().equalsIgnoreCase(Constants.microServiceProject)){
            if(microServiceRabbitMqDto.getEvent().equals(Constants.createEvent)) {
                microServiceManager.createMicroServiceEnvironment(microServiceRabbitMqDto.getMicroServiceEnvironmentNew());
            }else if (microServiceRabbitMqDto.getEvent().equals(Constants.updateEvent)) {
                microServiceManager.updateMicroServiceEnvironment(microServiceRabbitMqDto.getMicroServiceEnvironmentNew(),microServiceRabbitMqDto.getMicroServiceEnvironmentOld());
            } else{
                log.error("Unidentified Event!");
            }
        }else{
            log.error("Unidentified source!");
        }

    }
}
