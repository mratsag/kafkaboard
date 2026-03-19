package com.muratsag.kafkaboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicInfoDto {
    private String name;
    private int partitionCount;
    private int replicationFactor;
}