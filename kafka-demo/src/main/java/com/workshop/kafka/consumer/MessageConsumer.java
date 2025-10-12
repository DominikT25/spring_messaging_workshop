package com.workshop.kafka.consumer;

import com.workshop.kafka.metrics.MetricsService;
import com.workshop.kafka.model.WorkshopMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer service pro zpracování zpráv z Kafka.
 * Implementuje retry logiku s exponential backoff a DLQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {

    private final MetricsService metricsService;
    private final KafkaTemplate<String, WorkshopMessage> kafkaTemplate;
    private final Random random = new Random();
    private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();
    
    private int processedCount = 0;

    @Value("${workshop.kafka.dlq-topic}")
    private String dlqTopicName;

    @Value("${workshop.kafka.max-retries:3}")
    private int maxRetries;

    @Value("${workshop.kafka.simulate-failures:false}")
    private boolean simulateFailures;

    @Value("${workshop.kafka.failure-rate:0.1}")
    private double failureRate;

    /**
     * Listener pro hlavní topic.
     */
    @KafkaListener(topics = "${workshop.kafka.topic}", groupId = "workshop-consumer-group")
    public void receiveMessage(@Payload WorkshopMessage message,
                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                              @Header(KafkaHeaders.OFFSET) long offset,
                              Acknowledgment acknowledgment) {
        
        long startTime = System.nanoTime();
        String messageKey = partition + ":" + offset;
        
        try {
            // Zpracování zprávy
            processMessage(message);
            
            // ACK - úspěšné zpracování
            acknowledgment.acknowledge();
            
            // Odstranění z retry mapy
            retryCountMap.remove(messageKey);
            
            long latency = System.nanoTime() - startTime;
            metricsService.recordMessageConsumed(latency);
            
            processedCount++;
            if (processedCount % 100 == 0) {
                log.info("  Zpracováno: {} zpráv", processedCount);
            }
            
        } catch (Exception e) {
            // Získání retry počtu
            int currentRetryCount = retryCountMap.getOrDefault(messageKey, 0);
            
            if (currentRetryCount < maxRetries) {
                // Retry s exponential backoff
                retryCountMap.put(messageKey, currentRetryCount + 1);
                
                long backoffMs = (long) Math.pow(2, currentRetryCount) * 1000;
                log.warn("⚠ Chyba zpracování zprávy {} (pokus {}/{}): {}. Backoff: {} ms", 
                        message.getId(), currentRetryCount + 1, maxRetries, e.getMessage(), backoffMs);
                
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                
                // NEPOTVRZUJEME offset - zpráva bude znovu načtena
                
            } else {
                // Přesun do DLQ
                log.error("✗ Zpráva {} přesunuta do DLQ po {} pokusech", 
                        message.getId(), maxRetries);
                
                sendToDLQ(message, e.getMessage());
                
                // ACK offset po přesunu do DLQ
                acknowledgment.acknowledge();
                retryCountMap.remove(messageKey);
                metricsService.recordMessageFailed();
            }
        }
    }

    /**
     * Listener pro DLQ topic - pouze logování.
     */
    @KafkaListener(topics = "${workshop.kafka.dlq-topic}", groupId = "workshop-dlq-consumer-group")
    public void receiveDLQMessage(@Payload WorkshopMessage message,
                                  Acknowledgment acknowledgment) {
        
        log.info("📥 DLQ: Přijata zpráva {} do Dead Letter Queue", message.getId());
        
        // ACK zprávy v DLQ
        acknowledgment.acknowledge();
    }

    /**
     * Zpracování zprávy s možností simulace chyb.
     */
    private void processMessage(WorkshopMessage message) throws Exception {
        // Simulace náhodných chyb
        if (simulateFailures && random.nextDouble() < failureRate) {
            throw new RuntimeException("Simulovaná chyba zpracování");
        }
        
        // Simulace zpracování
        Thread.sleep(1);
        
        log.debug("✓ Zpracována zpráva: {}", message.getId());
    }

    /**
     * Odeslání zprávy do DLQ.
     */
    private void sendToDLQ(WorkshopMessage message, String error) {
        try {
            message.setContent(message.getContent() + " [ERROR: " + error + "]");
            message.setRetryCount(maxRetries);
            
            kafkaTemplate.send(dlqTopicName, "dlq-" + message.getId(), message);
        } catch (Exception e) {
            log.error("Chyba při odesílání do DLQ: {}", e.getMessage());
        }
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void resetProcessedCount() {
        this.processedCount = 0;
        this.retryCountMap.clear();
    }
}

