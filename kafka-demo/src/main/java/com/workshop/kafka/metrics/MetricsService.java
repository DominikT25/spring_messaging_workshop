package com.workshop.kafka.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service pro měření a sledování výkonu messaging operací.
 */
@Service
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    private long startTime;
    
    private final Counter messagesProducedCounter;
    private final Counter messagesConsumedCounter;
    private final Counter messagesFailedCounter;
    private final Timer producerLatencyTimer;
    private final Timer consumerLatencyTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.startTime = System.currentTimeMillis();
        
        // Inicializace counterů
        this.messagesProducedCounter = Counter.builder("messages.produced")
                .description("Total number of messages produced")
                .tag("broker", "kafka")
                .register(meterRegistry);
        
        this.messagesConsumedCounter = Counter.builder("messages.consumed")
                .description("Total number of messages consumed")
                .tag("broker", "kafka")
                .register(meterRegistry);
        
        this.messagesFailedCounter = Counter.builder("messages.failed")
                .description("Total number of failed messages")
                .tag("broker", "kafka")
                .register(meterRegistry);
        
        // Inicializace timerů
        this.producerLatencyTimer = Timer.builder("messages.producer.latency")
                .description("Producer latency")
                .tag("broker", "kafka")
                .register(meterRegistry);
        
        this.consumerLatencyTimer = Timer.builder("messages.consumer.latency")
                .description("Consumer latency")
                .tag("broker", "kafka")
                .register(meterRegistry);
    }

    public void recordMessageProduced(long latencyNanos) {
        messagesProducedCounter.increment();
        producerLatencyTimer.record(latencyNanos, TimeUnit.NANOSECONDS);
        latencies.add(latencyNanos);
    }

    public void recordMessageConsumed(long latencyNanos) {
        messagesConsumedCounter.increment();
        consumerLatencyTimer.record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    public void recordMessageFailed() {
        messagesFailedCounter.increment();
    }

    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
        this.latencies.clear();
    }

    public PerformanceSummary getSummary() {
        long duration = System.currentTimeMillis() - startTime;
        double durationSeconds = duration / 1000.0;
        
        long produced = (long) messagesProducedCounter.count();
        long consumed = (long) messagesConsumedCounter.count();
        long failed = (long) messagesFailedCounter.count();
        
        double throughput = produced / durationSeconds;
        double successRate = produced > 0 ? (consumed * 100.0 / produced) : 0;
        
        LatencyStats latencyStats = calculateLatencyStats();
        
        return PerformanceSummary.builder()
                .totalProduced(produced)
                .totalConsumed(consumed)
                .totalFailed(failed)
                .durationSeconds(durationSeconds)
                .throughputMsgPerSec(throughput)
                .successRatePercent(successRate)
                .latencyStats(latencyStats)
                .build();
    }

    private LatencyStats calculateLatencyStats() {
        if (latencies.isEmpty()) {
            return new LatencyStats(0, 0, 0, 0, 0, 0);
        }
        
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        
        long sum = sorted.stream().mapToLong(Long::longValue).sum();
        double avgNanos = (double) sum / sorted.size();
        long minNanos = sorted.get(0);
        long maxNanos = sorted.get(sorted.size() - 1);
        
        long p50Nanos = sorted.get(sorted.size() / 2);
        long p95Nanos = sorted.get((int) (sorted.size() * 0.95));
        long p99Nanos = sorted.get((int) (sorted.size() * 0.99));
        
        return new LatencyStats(
                avgNanos / 1_000_000.0,  // Convert to ms
                minNanos / 1_000_000.0,
                maxNanos / 1_000_000.0,
                p50Nanos / 1_000_000.0,
                p95Nanos / 1_000_000.0,
                p99Nanos / 1_000_000.0
        );
    }

    public void printSummary(String title) {
        PerformanceSummary summary = getSummary();
        
        log.info("=".repeat(60));
        log.info("{}", title);
        log.info("=".repeat(60));
        log.info("Celkem odesláno:     {}", summary.getTotalProduced());
        log.info("Celkem zpracováno:   {}", summary.getTotalConsumed());
        log.info("Celkem selhalo:      {}", summary.getTotalFailed());
        log.info("Doba běhu:           {:.2f} s", summary.getDurationSeconds());
        log.info("Propustnost:         {:.2f} msg/s", summary.getThroughputMsgPerSec());
        log.info("Úspěšnost:           {:.2f} %", summary.getSuccessRatePercent());
        log.info("");
        log.info("Latence (ms):");
        log.info("  Průměrná:          {:.2f}", summary.getLatencyStats().getAvgMs());
        log.info("  Minimální:         {:.2f}", summary.getLatencyStats().getMinMs());
        log.info("  Maximální:         {:.2f}", summary.getLatencyStats().getMaxMs());
        log.info("  P50 (medián):      {:.2f}", summary.getLatencyStats().getP50Ms());
        log.info("  P95:               {:.2f}", summary.getLatencyStats().getP95Ms());
        log.info("  P99:               {:.2f}", summary.getLatencyStats().getP99Ms());
        log.info("=".repeat(60));
    }
}

