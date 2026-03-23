package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.cluster.dto.TestClusterConnectionRequest;
import com.muratsag.kafkaboard.config.EncryptionService;
import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class KafkaAdminClientFactory {

    private final ConcurrentHashMap<UUID, AdminClient> clientCache = new ConcurrentHashMap<>();
    private final EncryptionService encryptionService;

    public AdminClient create(ClusterEntity cluster) {
        return clientCache.computeIfAbsent(cluster.getId(), id -> createNewClient(cluster));
    }

    public AdminClient createForTest(TestClusterConnectionRequest request) {
        Map<String, Object> config = buildConfig(
                request.getBootstrapServers(),
                request.getSecurityProtocol(),
                request.getSaslMechanism(),
                request.getSaslUsername(),
                request.getSaslPassword()
        );
        try {
            return AdminClient.create(config);
        } catch (Exception e) {
            throw new ClusterConnectionException(
                    "Kafka cluster'ına bağlanılamadı: " + request.getBootstrapServers() + " — " + e.getMessage());
        }
    }

    public void invalidate(UUID clusterId) {
        AdminClient client = clientCache.remove(clusterId);
        if (client != null) {
            client.close();
        }
    }

    @PreDestroy
    public void closeAll() {
        clientCache.values().forEach(AdminClient::close);
        clientCache.clear();
    }

    private AdminClient createNewClient(ClusterEntity cluster) {
        String password = null;
        if (cluster.getSaslPasswordEncrypted() != null) {
            password = encryptionService.decrypt(cluster.getSaslPasswordEncrypted());
        }
        Map<String, Object> config = buildConfig(
                cluster.getBootstrapServers(),
                cluster.getSecurityProtocol(),
                cluster.getSaslMechanism(),
                cluster.getSaslUsername(),
                password
        );
        try {
            return AdminClient.create(config);
        } catch (Exception e) {
            throw new ClusterConnectionException(
                    "Kafka cluster'ına bağlanılamadı: " + cluster.getBootstrapServers() + " — " + e.getMessage());
        }
    }

    private Map<String, Object> buildConfig(
            String bootstrapServers,
            String securityProtocol,
            String saslMechanism,
            String saslUsername,
            String saslPassword
    ) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("Bootstrap server adresi boş olamaz");
        }

        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        String protocol = securityProtocol != null ? securityProtocol : "PLAINTEXT";
        config.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, protocol);

        if ("SASL_SSL".equals(protocol) || "SASL_PLAINTEXT".equals(protocol)) {
            String mechanism = saslMechanism != null ? saslMechanism : "PLAIN";
            config.put(SaslConfigs.SASL_MECHANISM, mechanism);
            config.put(SaslConfigs.SASL_JAAS_CONFIG, buildJaasConfig(mechanism, saslUsername, saslPassword));
        }

        if ("SASL_SSL".equals(protocol)) {
            config.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "https");
        }

        return config;
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
