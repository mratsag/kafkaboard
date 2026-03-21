package com.muratsag.kafkaboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClusterHealthDto {
    private String clusterId;
    private int nodeCount;
    private List<NodeInfoDto> nodes;
    private int topicCount;
    private ClusterHealthStatus status;
}
