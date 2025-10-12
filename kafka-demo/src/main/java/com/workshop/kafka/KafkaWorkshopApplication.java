package com.workshop.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kafka Workshop - Spring Boot Application
 * 
 * Demonstrační aplikace pro práci s Apache Kafka včetně:
 * - Producer/Consumer pattern
 * - Retry mechanizmus s exponential backoff
 * - Dead Letter Topic (DLQ)
 * - Měření výkonu (latence, propustnost)
 */
@SpringBootApplication
@EnableKafka
@EnableScheduling
public class KafkaWorkshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaWorkshopApplication.class, args);
    }
}

