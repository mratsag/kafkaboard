package com.muratsag.kafkaboard.service;

import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class KafkaTopicMessageConsumerFactory {

    public KafkaConsumer<byte[], byte[]> create(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("Bootstrap server adresi boş olamaz");
        }

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafkaboard-read-" + UUID.randomUUID());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafkaboard-reader-" + UUID.randomUUID());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        try {
            return new KafkaConsumer<>(config);
        } catch (Exception e) {
            throw new ClusterConnectionException(
                    "Kafka cluster'ına bağlanılamadı: " + bootstrapServers + " — " + e.getMessage()
            );
        }
    }
}
