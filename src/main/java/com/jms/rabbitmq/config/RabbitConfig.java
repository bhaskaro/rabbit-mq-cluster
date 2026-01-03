package com.jms.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * Author : bhask
 * Created : 01-02-2026
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "topic.exchange";

    @Bean
    TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue orderQueue() {
        return QueueBuilder.durable("order.queue").build();
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable("payment.queue").build();
    }

    @Bean
    Queue auditQueue() {
        return QueueBuilder.durable("audit.queue").build();
    }

    @Bean
    Queue allQueue() {
        return QueueBuilder.durable("all.queue").build();
    }

    @Bean
    Binding orderBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(topicExchange())
                .with("order.*");
    }

    @Bean
    Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(topicExchange())
                .with("payment.*");
    }

    @Bean
    Binding auditBinding() {
        return BindingBuilder.bind(auditQueue())
                .to(topicExchange())
                .with("*.*");
    }

    @Bean
    Binding allBinding() {
        return BindingBuilder.bind(allQueue())
                .to(topicExchange())
                .with("#");
    }
}

