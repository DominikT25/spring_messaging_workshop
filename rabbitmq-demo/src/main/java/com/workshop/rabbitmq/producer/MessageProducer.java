package com.workshop.rabbitmq.producer;

import com.workshop.rabbitmq.metrics.MetricsService;
import com.workshop.rabbitmq.model.WorkshopMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Producer service pro odesílání zpráv do RabbitMQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final MetricsService metricsService;

    @Value("${workshop.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${workshop.rabbitmq.routing-key}")
    private String routingKey;

    /**
     * Odešle jednu zprávu.
     */
    public void sendMessage(WorkshopMessage message) {
        long startTime = System.nanoTime();
        
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            
            long latency = System.nanoTime() - startTime;
            metricsService.recordMessageProduced(latency);
            
            log.debug("Zpráva odeslána: {}", message.getId());
        } catch (Exception e) {
            log.error("Chyba při odesílání zprávy: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Odešle dávku zpráv.
     */
    public void sendBatch(int count) {
        log.info("Odesílání {} zpráv do RabbitMQ...", count);
        metricsService.resetStartTime();
        
        for (int i = 0; i < count; i++) {
            WorkshopMessage message = WorkshopMessage.builder()
                    .id((long) (i + 1))
                    .content("Test message " + (i + 1))
                    .timestamp(System.currentTimeMillis())
                    .broker("RabbitMQ")
                    .retryCount(0)
                    .build();
            
            sendMessage(message);
            
            if ((i + 1) % 100 == 0) {
                log.info("  Odesláno: {}/{} zpráv", i + 1, count);
            }
        }
        
        log.info("✓ Odesláno celkem {} zpráv", count);
        metricsService.printSummary("RabbitMQ Producer - Výsledky");
    }
}

