package com.workshop.rabbitmq.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSummary {
    private long totalProduced;
    private long totalConsumed;
    private long totalFailed;
    private double durationSeconds;
    private double throughputMsgPerSec;
    private double successRatePercent;
    private LatencyStats latencyStats;
}

