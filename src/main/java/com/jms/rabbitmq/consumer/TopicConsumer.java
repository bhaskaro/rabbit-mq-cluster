package com.jms.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TopicConsumer {

    @RabbitListener(queues = "order.queue")
    public void consumeOrder(Message message, Channel channel) throws Exception {
        handle("order.queue", message, channel);
    }

    @RabbitListener(queues = "payment.queue")
    public void consumePayment(Message message, Channel channel) throws Exception {
        handle("payment.queue", message, channel);
    }

    @RabbitListener(queues = "audit.queue")
    public void consumeAudit(Message message, Channel channel) throws Exception {
        handle("audit.queue", message, channel);
    }

    @RabbitListener(queues = "all.queue")
    public void consumeAll(Message message, Channel channel) throws Exception {
        handle("all.queue", message, channel);
    }

    private void handle(String queue, Message message, Channel channel) throws Exception {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        System.out.printf("[%s] received: %s%n", queue, payload);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}


