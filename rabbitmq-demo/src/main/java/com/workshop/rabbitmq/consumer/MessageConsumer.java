package com.workshop.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import com.workshop.rabbitmq.metrics.MetricsService;
import com.workshop.rabbitmq.model.WorkshopMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

/**
 * Consumer service pro zpracování zpráv z RabbitMQ.
 * Implementuje retry logiku a DLQ.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageConsumer {

    private final MetricsService metricsService;
    private final Random random = new Random();
    
    private int processedCount = 0;

    @Value("${workshop.rabbitmq.simulate-failures:false}")
    private boolean simulateFailures;

    @Value("${workshop.rabbitmq.failure-rate:0.1}")
    private double failureRate;

    /**
     * Listener pro hlavní frontu.
     */
    @RabbitListener(queues = "${workshop.rabbitmq.queue}")
    public void receiveMessage(@Payload WorkshopMessage message,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                              @Header(value = "x-retry-count", required = false) Integer retryCount,
                              Channel channel,
                              Message amqpMessage) throws IOException {
        
        long startTime = System.nanoTime();
        
        try {
            // Zpracování zprávy
            processMessage(message);
            
            // ACK - úspěšné zpracování
            channel.basicAck(deliveryTag, false);
            
            long latency = System.nanoTime() - startTime;
            metricsService.recordMessageConsumed(latency);
            
            processedCount++;
            if (processedCount % 100 == 0) {
                log.info("  Zpracováno: {} zpráv", processedCount);
            }
            
        } catch (Exception e) {
            // Získání retry počtu
            int currentRetryCount = retryCount != null ? retryCount : 0;
            int maxRetries = 3;
            
            if (currentRetryCount < maxRetries) {
                // NACK s requeue - zpráva bude znovu zpracována
                log.warn("⚠ Chyba zpracování zprávy {} (pokus {}/{}): {}", 
                        message.getId(), currentRetryCount + 1, maxRetries, e.getMessage());
                channel.basicNack(deliveryTag, false, true);
            } else {
                // NACK bez requeue - zpráva půjde do DLQ
                log.error("✗ Zpráva {} přesunuta do DLQ po {} pokusech", 
                        message.getId(), maxRetries);
                channel.basicNack(deliveryTag, false, false);
                metricsService.recordMessageFailed();
            }
        }
    }

    /**
     * Listener pro DLQ - pouze logování.
     */
    @RabbitListener(queues = "${workshop.rabbitmq.dlq}")
    public void receiveDLQMessage(@Payload WorkshopMessage message,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  Channel channel) throws IOException {
        
        log.info("📥 DLQ: Přijata zpráva {} do Dead Letter Queue", message.getId());
        
        // ACK zprávy v DLQ
        channel.basicAck(deliveryTag, false);
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

    public int getProcessedCount() {
        return processedCount;
    }

    public void resetProcessedCount() {
        this.processedCount = 0;
    }
}

