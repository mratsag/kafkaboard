package com.muratsag.kafkaboard.dto;

import lombok.Data;

@Data
public class CreateTopicRequest {
    private String bootstrapServers;
    private String topicName;
    private int partitions;
    private short replicationFactor;
}