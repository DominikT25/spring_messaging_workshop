package com.workshop.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ konfigurace pro workshop.
 * 
 * Nastavuje:
 * - Exchange (topic)
 * - Queue s DLQ
 * - Bindings
 * - Message converter (JSON)
 */
@Configuration
public class RabbitMQConfig {

    @Value("${workshop.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${workshop.rabbitmq.queue}")
    private String queueName;

    @Value("${workshop.rabbitmq.dlq}")
    private String dlqName;

    @Value("${workshop.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${workshop.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;

    /**
     * Topic Exchange pro směrování zpráv.
     */
    @Bean
    public TopicExchange workshopExchange() {
        return ExchangeBuilder
                .topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    /**
     * Hlavní fronta s konfigurací DLQ.
     */
    @Bean
    public Queue workshopQueue() {
        return QueueBuilder
                .durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    /**
     * Dead Letter Queue pro neúspěšné zprávy.
     */
    @Bean
    public Queue workshopDLQ() {
        return QueueBuilder
                .durable(dlqName)
                .build();
    }

    /**
     * Binding hlavní fronty na exchange.
     */
    @Bean
    public Binding workshopBinding(Queue workshopQueue, TopicExchange workshopExchange) {
        return BindingBuilder
                .bind(workshopQueue)
                .to(workshopExchange)
                .with(routingKey);
    }

    /**
     * Binding DLQ na exchange.
     */
    @Bean
    public Binding dlqBinding(Queue workshopDLQ, TopicExchange workshopExchange) {
        return BindingBuilder
                .bind(workshopDLQ)
                .to(workshopExchange)
                .with(dlqRoutingKey);
    }

    /**
     * JSON message converter.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate s JSON converterem.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Listener container factory s retry konfigurací.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}

