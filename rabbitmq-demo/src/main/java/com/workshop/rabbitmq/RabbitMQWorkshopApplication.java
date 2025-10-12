package com.workshop.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RabbitMQ Workshop - Spring Boot Application
 * 
 * Demonstrační aplikace pro práci s RabbitMQ včetně:
 * - Producer/Consumer pattern
 * - Retry mechanizmus
 * - Dead Letter Queue (DLQ)
 * - Měření výkonu (latence, propustnost)
 */
@SpringBootApplication
@EnableScheduling
public class RabbitMQWorkshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMQWorkshopApplication.class, args);
    }
}

