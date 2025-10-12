package com.workshop.rabbitmq.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatencyStats {
    private double avgMs;
    private double minMs;
    private double maxMs;
    private double p50Ms;
    private double p95Ms;
    private double p99Ms;
}

