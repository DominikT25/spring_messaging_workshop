package com.workshop.kafka.producer;

import com.workshop.kafka.metrics.MetricsService;
import com.workshop.kafka.model.WorkshopMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Producer service pro odesílání zpráv do Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProducer {

    private final KafkaTemplate<String, WorkshopMessage> kafkaTemplate;
    private final MetricsService metricsService;

    @Value("${workshop.kafka.topic}")
    private String topicName;

    /**
     * Odešle jednu zprávu.
     */
    public void sendMessage(WorkshopMessage message) {
        long startTime = System.nanoTime();
        
        try {
            String key = "msg-" + message.getId();
            
            CompletableFuture<SendResult<String, WorkshopMessage>> future = 
                    kafkaTemplate.send(topicName, key, message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    long latency = System.nanoTime() - startTime;
                    metricsService.recordMessageProduced(latency);
                    
                    log.debug("Zpráva odeslána: {} -> partition: {}, offset: {}", 
                            message.getId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Chyba při odesílání zprávy {}: {}", message.getId(), ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Chyba při odesílání zprávy: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Odešle dávku zpráv.
     */
    public void sendBatch(int count) {
        log.info("Odesílání {} zpráv do Kafka...", count);
        metricsService.resetStartTime();
        
        for (int i = 0; i < count; i++) {
            WorkshopMessage message = WorkshopMessage.builder()
                    .id((long) (i + 1))
                    .content("Test message " + (i + 1))
                    .timestamp(System.currentTimeMillis())
                    .broker("Kafka")
                    .retryCount(0)
                    .build();
            
            sendMessage(message);
            
            if ((i + 1) % 100 == 0) {
                log.info("  Odesláno: {}/{} zpráv", i + 1, count);
            }
        }
        
        // Flush všech zpráv
        kafkaTemplate.flush();
        
        log.info("✓ Odesláno celkem {} zpráv", count);
        
        // Počkat na dokončení všech asynchronních operací
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        metricsService.printSummary("Kafka Producer - Výsledky");
    }
}

