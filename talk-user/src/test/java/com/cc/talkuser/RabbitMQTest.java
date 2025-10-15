package com.cc.talkuser;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

public class RabbitMQTest {
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.72.128");
            factory.setPort(5672);
            factory.setUsername("bookTalk");
            factory.setPassword("bookTalk");
            factory.setVirtualHost("/bookTalk");

            System.out.println("Connecting to RabbitMQ...");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            System.out.println("RabbitMQ connection successful!");

            // Test queue declaration
            channel.queueDeclare("test.queue", true, false, false, null);
            System.out.println("Test queue created successfully!");

            channel.close();
            connection.close();
            System.out.println("Connection closed");

        } catch (Exception e) {
            System.err.println("RabbitMQ connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}