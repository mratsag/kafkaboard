package com.muratsag.kafkaboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicMessageDto {
    private int partition;
    private long offset;
    private String key;
    private String value;
    private long timestamp;
}
