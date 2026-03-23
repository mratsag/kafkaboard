package com.muratsag.kafkaboard.cluster.dto;

import lombok.Data;

@Data
public class CreateClusterRequest {
    private String name;
    private String bootstrapServers;
    private String securityProtocol = "PLAINTEXT";
    private String saslMechanism;
    private String saslUsername;
    private String saslPassword;
}
