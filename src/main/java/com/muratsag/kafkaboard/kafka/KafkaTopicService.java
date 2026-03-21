package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.dto.TopicInfoDto;
import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaTopicService {

    private final KafkaAdminClientFactory adminClientFactory;

    public List<TopicInfoDto> getTopics(String bootstrapServers) {
        try {
            AdminClient adminClient = adminClientFactory.create(bootstrapServers);

            ListTopicsOptions options = new ListTopicsOptions();
            options.listInternal(true);

            Set<String> topicNames = adminClient
                    .listTopics(options)
                    .names()
                    .get(5, TimeUnit.SECONDS);

            if (topicNames.isEmpty()) {
                return List.of();
            }

            Map<String, TopicDescription> descriptions = adminClient
                    .describeTopics(topicNames)
                    .allTopicNames()
                    .get(5, TimeUnit.SECONDS);

            return descriptions.values().stream()
                    .map(desc -> TopicInfoDto.builder()
                            .name(desc.name())
                            .partitionCount(desc.partitions().size())
                            .replicationFactor(desc.partitions().getFirst().replicas().size())
                            .build())
                    .toList();

        } catch (ClusterConnectionException e) {
            throw e;
        } catch (Exception e) {
            adminClientFactory.invalidate(bootstrapServers);
            throw new ClusterConnectionException("Topic listesi alınamadı — " + e.getMessage());
        }
    }

    public TopicInfoDto createTopic(String bootstrapServers, String topicName, int partitions, short replicationFactor) {
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("Topic adı boş olamaz");
        }
        if (partitions < 1) {
            throw new IllegalArgumentException("Partition sayısı en az 1 olmalı");
        }
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("Replication factor en az 1 olmalı");
        }

        try {
            AdminClient adminClient = adminClientFactory.create(bootstrapServers);
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            adminClient.createTopics(List.of(newTopic)).all().get(5, TimeUnit.SECONDS);

            return TopicInfoDto.builder()
                    .name(topicName)
                    .partitionCount(partitions)
                    .replicationFactor(replicationFactor)
                    .build();

        } catch (ClusterConnectionException e) {
            throw e;
        } catch (Exception e) {
            adminClientFactory.invalidate(bootstrapServers);
            throw new ClusterConnectionException("Topic oluşturulamadı: " + topicName + " — " + e.getMessage());
        }
    }

    public void deleteTopic(String bootstrapServers, String topicName) {
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("Topic adı boş olamaz");
        }

        try {
            AdminClient adminClient = adminClientFactory.create(bootstrapServers);
            adminClient.deleteTopics(List.of(topicName)).all().get(5, TimeUnit.SECONDS);

        } catch (ClusterConnectionException e) {
            throw e;
        } catch (Exception e) {
            adminClientFactory.invalidate(bootstrapServers);
            throw new ClusterConnectionException("Topic silinemedi: " + topicName + " — " + e.getMessage());
        }
    }
}
