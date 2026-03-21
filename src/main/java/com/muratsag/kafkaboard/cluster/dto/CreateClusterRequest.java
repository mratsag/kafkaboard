package com.muratsag.kafkaboard.cluster.dto;

import lombok.Data;

@Data
public class CreateClusterRequest {
    private String name;
    private String bootstrapServers;
}
