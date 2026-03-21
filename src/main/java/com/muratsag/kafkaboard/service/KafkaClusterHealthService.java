package com.muratsag.kafkaboard.service;

import com.muratsag.kafkaboard.dto.ClusterHealthDto;
import com.muratsag.kafkaboard.dto.ClusterHealthStatus;
import com.muratsag.kafkaboard.dto.NodeInfoDto;
import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaClusterHealthService {

    private final KafkaAdminClientFactory adminClientFactory;

    public ClusterHealthDto getClusterHealth(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("Bootstrap server adresi boş olamaz");
        }

        try {
            AdminClient adminClient = adminClientFactory.create(bootstrapServers);
            DescribeClusterResult clusterResult = adminClient.describeCluster();

            String clusterId = normalizeClusterId(clusterResult
                    .clusterId()
                    .get(5, TimeUnit.SECONDS));

            List<NodeInfoDto> nodes = clusterResult
                    .nodes()
                    .get(5, TimeUnit.SECONDS)
                    .stream()
                    .map(this::toNodeInfoDto)
                    .toList();

            ListTopicsOptions options = new ListTopicsOptions();
            options.listInternal(true);

            Set<String> topics = adminClient.listTopics(options)
                    .names()
                    .get(5, TimeUnit.SECONDS);

            return ClusterHealthDto.builder()
                    .clusterId(clusterId)
                    .nodeCount(nodes.size())
                    .nodes(nodes)
                    .topicCount(topics.size())
                    .status(resolveStatus(nodes.size()))
                    .build();

        } catch (ClusterConnectionException e) {
            adminClientFactory.invalidate(bootstrapServers);
            return buildUnhealthy();
        } catch (Exception e) {
            adminClientFactory.invalidate(bootstrapServers);
            return buildUnhealthy();
        }
    }

    private NodeInfoDto toNodeInfoDto(Node node) {
        return NodeInfoDto.builder()
                .nodeId(node.id())
                .host(node.host())
                .port(node.port())
                .rack(node.rack())
                .build();
    }

    private ClusterHealthStatus resolveStatus(int nodeCount) {
        if (nodeCount > 0) {
            return ClusterHealthStatus.HEALTHY;
        }
        return ClusterHealthStatus.DEGRADED;
    }

    private String normalizeClusterId(String clusterId) {
        if (clusterId == null) {
            return null;
        }
        if (clusterId.startsWith("Some(") && clusterId.endsWith(")")) {
            return clusterId.substring(5, clusterId.length() - 1);
        }
        return clusterId;
    }

    private ClusterHealthDto buildUnhealthy() {
        return ClusterHealthDto.builder()
                .clusterId(null)
                .nodeCount(0)
                .nodes(List.of())
                .topicCount(0)
                .status(ClusterHealthStatus.UNHEALTHY)
                .build();
    }
}
