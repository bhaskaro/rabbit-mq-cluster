package com.oggu.jms.rabbitmq.producer;

import com.oggu.jms.rabbitmq.config.RabbitConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Component
public class TopicProducer {

    private static final String[] ROUTING_KEYS = {
            "order.created",
            "order.updated",
            "payment.success",
            "payment.failed"
    };

    private final RabbitTemplate rabbitTemplate;
    private final Executor rabbitExecutor;

    // ✅ Constructor Injection + Qualifier (FIXES YOUR ERROR)
    public TopicProducer(RabbitTemplate rabbitTemplate,
                         @Qualifier("rabbitExecutor") Executor rabbitExecutor) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitExecutor = rabbitExecutor;
    }

    @Scheduled(fixedRateString = "${app.messaging.producer.fixed-rate-ms}")
    public void publish() {

        List<CompletableFuture<Void>> futures = Stream.of(ROUTING_KEYS)
                .map(routingKey ->
                        CompletableFuture.runAsync(() -> {
                            String message = "Message at " + Instant.now();

                            rabbitTemplate.convertAndSend(
                                    RabbitConfig.EXCHANGE,
                                    routingKey,
                                    message,
                                    msg -> {
                                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                                        return msg;
                                    });

                            System.out.println("Sent → " + routingKey + " --- " + message);

                        }, rabbitExecutor)   // 👈 IMPORTANT
                ).toList();

        // Optional: wait for all (if needed)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }


//    @Scheduled(fixedRateString = "${app.messaging.producer.fixed-rate-ms}")
//    public void publish() {
//        String routingKey = ROUTING_KEYS[random.nextInt(ROUTING_KEYS.length)];
//        String message = "Message at " + Instant.now();
//
//        rabbitTemplate.convertAndSend(
//                RabbitConfig.EXCHANGE,
//                routingKey,
//                message,
//                msg -> {
//                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
//                    return msg;
//                });
//
//        System.out.println("Sent → " + routingKey + " : " + message);
//    }
}
