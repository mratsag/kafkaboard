package com.muratsag.kafkaboard.cluster.dto;

import lombok.Data;

@Data
public class TestClusterConnectionRequest {
    private String bootstrapServers;
}
