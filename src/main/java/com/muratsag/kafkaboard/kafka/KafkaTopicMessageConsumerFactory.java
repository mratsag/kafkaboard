package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.config.EncryptionService;
import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaTopicMessageConsumerFactory {

    private final EncryptionService encryptionService;

    public KafkaConsumer<byte[], byte[]> create(ClusterEntity cluster) {
        String password = null;
        if (cluster.getSaslPasswordEncrypted() != null) {
            password = encryptionService.decrypt(cluster.getSaslPasswordEncrypted());
        }

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cluster.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafkaboard-read-" + UUID.randomUUID());
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafkaboard-reader-" + UUID.randomUUID());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "none");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        String protocol = cluster.getSecurityProtocol() != null ? cluster.getSecurityProtocol() : "PLAINTEXT";
        config.put("security.protocol", protocol);

        if ("SASL_SSL".equals(protocol) || "SASL_PLAINTEXT".equals(protocol)) {
            String mechanism = cluster.getSaslMechanism() != null ? cluster.getSaslMechanism() : "PLAIN";
            config.put(SaslConfigs.SASL_MECHANISM, mechanism);
            config.put(SaslConfigs.SASL_JAAS_CONFIG, buildJaasConfig(mechanism, cluster.getSaslUsername(), password));
        }

        if ("SASL_SSL".equals(protocol)) {
            config.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
        }

        try {
            return new KafkaConsumer<>(config);
        } catch (Exception e) {
            throw new ClusterConnectionException(
                    "Kafka cluster'ına bağlanılamadı: " + cluster.getBootstrapServers() + " — " + e.getMessage());
        }
    }

    private String buildJaasConfig(String mechanism, String username, String password) {
        String loginModule = switch (mechanism) {
            case "SCRAM-SHA-256", "SCRAM-SHA-512" ->
                    "org.apache.kafka.common.security.scram.ScramLoginModule";
            default ->
                    "org.apache.kafka.common.security.plain.PlainLoginModule";
        };
        return loginModule + " required username=\"" + username + "\" password=\"" + password + "\";";
    }
}
