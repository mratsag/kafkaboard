package com.muratsag.kafkaboard.cluster.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ClusterDto {
    private UUID id;
    private String name;
    private String bootstrapServers;
    private String securityProtocol;
    private String saslMechanism;
    private String saslUsername;
    private LocalDateTime createdAt;
}
