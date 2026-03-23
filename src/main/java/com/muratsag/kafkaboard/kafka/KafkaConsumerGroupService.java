package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.dto.ConsumerGroupInfoDto;
import com.muratsag.kafkaboard.dto.PartitionLagDto;
import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KafkaConsumerGroupService {

    private final KafkaAdminClientFactory adminClientFactory;

    public List<ConsumerGroupInfoDto> getConsumerGroupLag(ClusterEntity cluster) {
        try {
            AdminClient adminClient = adminClientFactory.create(cluster);

            List<String> groupIds = adminClient
                    .listConsumerGroups()
                    .all()
                    .get(5, TimeUnit.SECONDS)
                    .stream()
                    .map(ConsumerGroupListing::groupId)
                    .toList();

            if (groupIds.isEmpty()) {
                return List.of();
            }

            Map<String, ConsumerGroupDescription> groupDescriptions = adminClient
                    .describeConsumerGroups(groupIds)
                    .all()
                    .get(5, TimeUnit.SECONDS);

            Map<String, ListConsumerGroupOffsetsResult> offsetResults = new HashMap<>();
            for (String groupId : groupIds) {
                offsetResults.put(groupId, adminClient.listConsumerGroupOffsets(groupId));
            }

            List<ConsumerGroupInfoDto> result = new ArrayList<>();

            for (String groupId : groupIds) {
                Map<TopicPartition, OffsetAndMetadata> committedOffsets = offsetResults
                        .get(groupId)
                        .partitionsToOffsetAndMetadata()
                        .get(5, TimeUnit.SECONDS);

                if (committedOffsets.isEmpty()) {
                    continue;
                }

                Map<TopicPartition, Long> endOffsets = adminClient
                        .listOffsets(committedOffsets.keySet().stream()
                                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest())))
                        .all()
                        .get(5, TimeUnit.SECONDS)
                        .entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().offset()));

                List<PartitionLagDto> partitionLags = new ArrayList<>();
                long totalLag = 0;

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committedOffsets.entrySet()) {
                    TopicPartition topicPartition = entry.getKey();
                    long committed = entry.getValue().offset();
                    long end = endOffsets.getOrDefault(topicPartition, committed);
                    long lag = Math.max(0, end - committed);
                    totalLag += lag;

                    partitionLags.add(PartitionLagDto.builder()
                            .topic(topicPartition.topic())
                            .partition(topicPartition.partition())
                            .currentOffset(committed)
                            .endOffset(end)
                            .lag(lag)
                            .build());
                }

                result.add(ConsumerGroupInfoDto.builder()
                        .groupId(groupId)
                        .state(groupDescriptions.get(groupId).state().toString())
                        .totalLag(totalLag)
                        .partitionLags(partitionLags)
                        .build());
            }

            return result;

        } catch (ClusterConnectionException e) {
            adminClientFactory.invalidate(cluster.getId());
            throw e;
        } catch (Exception e) {
            adminClientFactory.invalidate(cluster.getId());
            throw new ClusterConnectionException("Consumer group bilgisi alınamadı — " + e.getMessage());
        }
    }
}
