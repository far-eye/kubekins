package co.fareye.microservicemanager.config.aqmp;

import co.fareye.microservicemanager.core.service.MicroServiceQueueListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MicroServiceQueue{


    @Bean(name = "MICROSERVICE_QUEUE")
    Queue queue() {
        return new Queue(getQueueName(), true);
    }

    @Bean(name = "MICROSERVICE_EXCHANGE")
    TopicExchange exchange() {
        return new TopicExchange("microservice-exchange");
    }

    @Bean(name = "MICROSERVICE_BINDING")
    Binding binding(@Qualifier("MICROSERVICE_QUEUE") Queue queue, @Qualifier("MICROSERVICE_EXCHANGE") TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(getQueueName());
    }

    @Bean(name = "MICROSERVICE_CONTAINER")
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, @Qualifier("MICROSERVICE_ADAPTER") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(getQueueName());
        container.setMessageListener(listenerAdapter);
        container.setConcurrentConsumers(1);
        return container;
    }

    @Bean(name = "MICROSERVICE_ADAPTER")
    MessageListenerAdapter listenerAdapter(MicroServiceQueueListener microServiceQueueListener) {
        return new MessageListenerAdapter(microServiceQueueListener, "microServiceQueueListener");
    }

    public String getQueueName() {
        return "microservice";
    }
}
