package com.jms.rabbitmq;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopicProducer {

    private static final String RABBITMQ_HOST = "192.168.1.222";

    private static final String EXCHANGE = "topic.exchange";
    private static final String[] ROUTING_KEYS = {
            "order.created",
            "order.updated",
            "payment.success",
            "payment.failed"
    };

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("app_vhost");

        Connection connection = factory.newConnection();

        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 4; i++) {
            final int threadId = i;
            executor.submit(() -> publish(connection, threadId));
        }
    }

    private static void publish(Connection connection, int threadId) {
        try (Channel channel = connection.createChannel()) {

            int count = 0;
            while (true) {
                String routingKey = ROUTING_KEYS[count % ROUTING_KEYS.length];
                String message = "Message-" + count + " from Producer-" + threadId;

                channel.basicPublish(
                        EXCHANGE,
                        routingKey,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8));

                System.out.printf("Producer-%d sent [%s]: %s%n",
                        threadId, routingKey, message);

                count++;
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
