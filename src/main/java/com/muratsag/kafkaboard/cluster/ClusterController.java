package com.muratsag.kafkaboard.cluster;

import com.muratsag.kafkaboard.auth.AuthenticatedUser;
import com.muratsag.kafkaboard.cluster.dto.ClusterDto;
import com.muratsag.kafkaboard.cluster.dto.CreateClusterRequest;
import com.muratsag.kafkaboard.dto.ClusterHealthDto;
import com.muratsag.kafkaboard.dto.ConsumerGroupInfoDto;
import com.muratsag.kafkaboard.dto.CreateTopicRequest;
import com.muratsag.kafkaboard.dto.TopicInfoDto;
import com.muratsag.kafkaboard.dto.TopicMessageDto;
import com.muratsag.kafkaboard.kafka.KafkaClusterHealthService;
import com.muratsag.kafkaboard.kafka.KafkaConsumerGroupService;
import com.muratsag.kafkaboard.kafka.KafkaTopicMessageService;
import com.muratsag.kafkaboard.kafka.KafkaTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clusters")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;
    private final KafkaTopicService kafkaTopicService;
    private final KafkaConsumerGroupService kafkaConsumerGroupService;
    private final KafkaTopicMessageService kafkaTopicMessageService;
    private final KafkaClusterHealthService kafkaClusterHealthService;

    @PostMapping
    public ResponseEntity<ClusterDto> createCluster(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody CreateClusterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clusterService.createCluster(user.getId(), request));
    }

    @GetMapping
    public List<ClusterDto> getClusters(@AuthenticationPrincipal AuthenticatedUser user) {
        return clusterService.getClusters(user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        clusterService.deleteCluster(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/health")
    public ClusterHealthDto getClusterHealth(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        ClusterEntity cluster = clusterService.getOwnedCluster(id, user.getId());
        return kafkaClusterHealthService.getClusterHealth(cluster.getBootstrapServers());
    }

    @GetMapping("/{id}/topics")
    public List<TopicInfoDto> getTopics(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        ClusterEntity cluster = clusterService.getOwnedCluster(id, user.getId());
        return kafkaTopicService.getTopics(cluster.getBootstrapServers());
    }

    @PostMapping("/{id}/topics")
    public ResponseEntity<TopicInfoDto> createTopic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @RequestBody CreateTopicRequest request
    ) {
        ClusterEntity cluster = clusterService.getOwnedCluster(id, user.getId());
        TopicInfoDto created = kafkaTopicService.createTopic(
                cluster.getBootstrapServers(),
                request.getTopicName(),
                request.getPartitions(),
                request.getReplicationFactor()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}/topics/{topicName}")
    public ResponseEntity<Void> deleteTopic(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @PathVariable String topicName
    ) {
        ClusterEntity cluster = clusterService.getOwnedCluster(id, user.getId());
        kafkaTopicService.deleteTopic(cluster.getBootstrapServers(), topicName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/topics/{topicName}/messages")
    public List<TopicMessageDto> getTopicMessages(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id,
            @PathVariable String topicName,
            @RequestParam(defaultValue = "10") int limit
    ) {
        ClusterEntity cluster = clusterService.getOwnedCluster(id, user.getId());
        return kafkaTopicMessageService.getLatestMessages(cluster.getBootstrapServers(), topicName, limit);
    }

    @GetMapping("/{id}/consumer-groups")
    public List<ConsumerGroupInfoDto> getConsumerGroups(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id
    ) {
        ClusterEntity cluster = clusterService.getOwnedCluster(id, user.getId());
        return kafkaConsumerGroupService.getConsumerGroupLag(cluster.getBootstrapServers());
    }
}
