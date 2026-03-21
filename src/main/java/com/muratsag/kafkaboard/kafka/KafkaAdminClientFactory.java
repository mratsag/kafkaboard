package com.muratsag.kafkaboard.kafka;

import com.muratsag.kafkaboard.exception.ClusterConnectionException;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KafkaAdminClientFactory {

    private final ConcurrentHashMap<String, AdminClient> clientCache = new ConcurrentHashMap<>();

    public AdminClient create(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("Bootstrap server adresi boş olamaz");
        }

        return clientCache.computeIfAbsent(bootstrapServers, this::createNewClient);
    }

    private AdminClient createNewClient(String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        try {
            return AdminClient.create(config);
        } catch (Exception e) {
            throw new ClusterConnectionException(
                    "Kafka cluster'ına bağlanılamadı: " + bootstrapServers + " — " + e.getMessage()
            );
        }
    }

    public void invalidate(String bootstrapServers) {
        AdminClient client = clientCache.remove(bootstrapServers);
        if (client != null) {
            client.close();
        }
    }

    @PreDestroy
    public void closeAll() {
        clientCache.values().forEach(AdminClient::close);
        clientCache.clear();
    }
}
