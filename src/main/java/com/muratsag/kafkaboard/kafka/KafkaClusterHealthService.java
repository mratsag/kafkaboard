package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.cluster.dto.TestClusterConnectionRequest;
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

    public ClusterHealthDto getClusterHealth(ClusterEntity cluster) {
        try {
            AdminClient adminClient = adminClientFactory.create(cluster);
            return fetchHealth(adminClient);
        } catch (ClusterConnectionException e) {
            adminClientFactory.invalidate(cluster.getId());
            return buildUnhealthy();
        } catch (Exception e) {
            adminClientFactory.invalidate(cluster.getId());
            return buildUnhealthy();
        }
    }

    public String getConnectionError(ClusterEntity cluster) {
        try {
            AdminClient adminClient = adminClientFactory.create(cluster);
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            clusterResult.clusterId().get(5, TimeUnit.SECONDS);
            clusterResult.nodes().get(5, TimeUnit.SECONDS);
            return null;
        } catch (Exception e) {
            adminClientFactory.invalidate(cluster.getId());
            return e.getMessage();
        }
    }

    public ClusterHealthDto testConnection(TestClusterConnectionRequest request) {
        AdminClient adminClient = adminClientFactory.createForTest(request);
        try {
            return fetchHealth(adminClient);
        } catch (Exception e) {
            return buildUnhealthy();
        } finally {
            adminClient.close();
        }
    }

    public String getTestConnectionError(TestClusterConnectionRequest request) {
        AdminClient adminClient = adminClientFactory.createForTest(request);
        try {
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            clusterResult.clusterId().get(5, TimeUnit.SECONDS);
            clusterResult.nodes().get(5, TimeUnit.SECONDS);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            adminClient.close();
        }
    }

    private ClusterHealthDto fetchHealth(AdminClient adminClient) throws Exception {
        DescribeClusterResult clusterResult = adminClient.describeCluster();

        String clusterId = normalizeClusterId(clusterResult.clusterId().get(5, TimeUnit.SECONDS));

        List<NodeInfoDto> nodes = clusterResult.nodes().get(5, TimeUnit.SECONDS).stream()
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
        return nodeCount > 0 ? ClusterHealthStatus.HEALTHY : ClusterHealthStatus.DEGRADED;
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
