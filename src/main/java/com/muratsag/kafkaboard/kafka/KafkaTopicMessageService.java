package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.dto.TopicMessageDto;
import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaTopicMessageService {

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(1);
    private static final int MAX_EMPTY_POLLS = 3;

    private final KafkaAdminClientFactory adminClientFactory;
    private final KafkaTopicMessageConsumerFactory consumerFactory;

    public List<TopicMessageDto> getLatestMessages(ClusterEntity cluster, String topicName, int limit) {
        if (topicName == null || topicName.isBlank()) {
            throw new IllegalArgumentException("Topic adı boş olamaz");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit en az 1 olmalı");
        }

        validateTopicExists(cluster, topicName);

        try (KafkaConsumer<byte[], byte[]> consumer = consumerFactory.create(cluster)) {
            List<PartitionInfo> partitionInfos = consumer.partitionsFor(topicName, Duration.ofSeconds(5));
            if (partitionInfos == null || partitionInfos.isEmpty()) {
                throw new IllegalArgumentException("Topic bulunamadı: " + topicName);
            }

            List<TopicPartition> partitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .toList();

            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitions, Duration.ofSeconds(5));
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions, Duration.ofSeconds(5));

            List<TopicMessageDto> messages = new ArrayList<>();
            for (TopicPartition partition : partitions) {
                long beginningOffset = beginningOffsets.getOrDefault(partition, 0L);
                long endOffset = endOffsets.getOrDefault(partition, beginningOffset);
                if (endOffset <= beginningOffset) {
                    continue;
                }

                long startOffset = Math.max(beginningOffset, endOffset - limit);
                messages.addAll(readPartitionMessages(consumer, partition, startOffset, endOffset));
            }

            return messages.stream()
                    .sorted(Comparator.comparingLong(TopicMessageDto::getTimestamp)
                            .reversed()
                            .thenComparing(Comparator.comparingLong(TopicMessageDto::getOffset).reversed())
                            .thenComparing(Comparator.comparingInt(TopicMessageDto::getPartition).reversed()))
                    .limit(limit)
                    .toList();

        } catch (IllegalArgumentException | ClusterConnectionException e) {
            throw e;
        } catch (Exception e) {
            adminClientFactory.invalidate(cluster.getId());
            throw new ClusterConnectionException("Topic mesajları alınamadı: " + topicName + " — " + e.getMessage());
        }
    }

    private void validateTopicExists(ClusterEntity cluster, String topicName) {
        try {
            AdminClient adminClient = adminClientFactory.create(cluster);
            adminClient.describeTopics(List.of(topicName)).allTopicNames().get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() != null
                    && "UnknownTopicOrPartitionException".equals(e.getCause().getClass().getSimpleName())) {
                throw new IllegalArgumentException("Topic bulunamadı: " + topicName);
            }

            adminClientFactory.invalidate(cluster.getId());
            throw new ClusterConnectionException("Topic mesajları alınamadı: " + topicName + " — " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            adminClientFactory.invalidate(cluster.getId());
            throw new ClusterConnectionException("Topic mesajları alınamadı: " + topicName + " — " + e.getMessage());
        }
    }

    private List<TopicMessageDto> readPartitionMessages(
            KafkaConsumer<byte[], byte[]> consumer,
            TopicPartition partition,
            long startOffset,
            long endOffset
    ) {
        consumer.assign(List.of(partition));
        consumer.seek(partition, startOffset);

        List<TopicMessageDto> messages = new ArrayList<>();
        int emptyPollCount = 0;

        while (consumer.position(partition) < endOffset && emptyPollCount < MAX_EMPTY_POLLS) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(POLL_TIMEOUT);
            List<ConsumerRecord<byte[], byte[]>> partitionRecords = records.records(partition);

            if (partitionRecords.isEmpty()) {
                emptyPollCount++;
                continue;
            }

            emptyPollCount = 0;
            for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
                if (record.offset() >= endOffset) {
                    break;
                }

                messages.add(TopicMessageDto.builder()
                        .partition(record.partition())
                        .offset(record.offset())
                        .key(toUtf8(record.key()))
                        .value(toUtf8(record.value()))
                        .timestamp(record.timestamp())
                        .build());
            }
        }

        return messages;
    }

    private String toUtf8(byte[] value) {
        if (value == null) {
            return null;
        }
        return new String(value, StandardCharsets.UTF_8);
    }
}
