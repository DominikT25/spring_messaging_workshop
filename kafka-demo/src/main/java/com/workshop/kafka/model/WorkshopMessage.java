package com.workshop.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Model zprávy pro workshop.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopMessage implements Serializable {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("broker")
    private String broker;
    
    @JsonProperty("retry_count")
    private Integer retryCount;
    
    public WorkshopMessage(Long id, String content) {
        this.id = id;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.broker = "RabbitMQ";
        this.retryCount = 0;
    }
}

