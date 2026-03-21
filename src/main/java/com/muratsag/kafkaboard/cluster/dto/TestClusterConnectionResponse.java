package com.muratsag.kafkaboard.cluster.dto;

import com.muratsag.kafkaboard.dto.ClusterHealthStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestClusterConnectionResponse {
    private ClusterHealthStatus status;
    private Integer nodeCount;
    private String clusterId;
    private String error;
}
