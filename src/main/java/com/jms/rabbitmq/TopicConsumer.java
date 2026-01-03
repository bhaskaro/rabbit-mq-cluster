package com.jms.rabbitmq;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopicConsumer {

    private static final String RABBITMQ_HOST = "192.168.1.222";

    private static final String[] QUEUES = {
            "order.queue",
            "payment.queue",
            "audit.queue",
            "all.queue"
    };

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("app_vhost");

        Connection connection = factory.newConnection();

        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (String queue : QUEUES) {
            executor.submit(() -> consume(connection, queue));
        }
    }

    private static void consume(Connection connection, String queue) {
        try {
            Channel channel = connection.createChannel();
            channel.basicQos(10);

            DeliverCallback callback = (tag, delivery) -> {
                String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.printf("Queue [%s] received: %s%n", queue, msg);

                try {
                    Thread.sleep(300);// simulate work
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } 
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(queue, false, callback, tag -> {});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

