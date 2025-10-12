package com.workshop.rabbitmq.controller;

import com.workshop.rabbitmq.consumer.MessageConsumer;
import com.workshop.rabbitmq.metrics.MetricsService;
import com.workshop.rabbitmq.metrics.PerformanceSummary;
import com.workshop.rabbitmq.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller pro ovládání workshopu.
 */
@RestController
@RequestMapping("/api/workshop")
@RequiredArgsConstructor
@Slf4j
public class WorkshopController {

    private final MessageProducer messageProducer;
    private final MessageConsumer messageConsumer;
    private final MetricsService metricsService;

    /**
     * Odeslání dávky zpráv.
     * 
     * POST /api/workshop/send?count=1000
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessages(
            @RequestParam(defaultValue = "1000") int count) {
        
        log.info("📤 Požadavek na odeslání {} zpráv", count);
        
        try {
            messageConsumer.resetProcessedCount();
            messageProducer.sendBatch(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Odesláno " + count + " zpráv");
            response.put("count", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chyba při odesílání zpráv", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Získání metrik výkonu.
     * 
     * GET /api/workshop/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<PerformanceSummary> getMetrics() {
        PerformanceSummary summary = metricsService.getSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Reset metrik.
     * 
     * POST /api/workshop/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        metricsService.resetStartTime();
        messageConsumer.resetProcessedCount();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Metriky resetovány");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check.
     * 
     * GET /api/workshop/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("broker", "RabbitMQ");
        response.put("application", "rabbitmq-workshop-demo");
        
        return ResponseEntity.ok(response);
    }
}

