package com.oggu.jms.rabbitmq.producer;

import com.oggu.jms.rabbitmq.config.RabbitConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;

@Component
public class TopicProducer {

    private static final String[] ROUTING_KEYS = {
            "order.created",
            "order.updated",
            "payment.success",
            "payment.failed"
    };

    private final Random random = new Random();
    private final RabbitTemplate rabbitTemplate;

    public TopicProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedRateString = "${app.messaging.producer.fixed-rate-ms}")
    public void publish() {
        String routingKey = ROUTING_KEYS[random.nextInt(ROUTING_KEYS.length)];
        String message = "Message at " + Instant.now();

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                routingKey,
                message,
                msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return msg;
                });

        System.out.println("Sent → " + routingKey + " : " + message);
    }
}
