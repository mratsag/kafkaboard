package com.muratsag.kafkaboard.service;

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

    // bootstrapServers → AdminClient cache
    private final ConcurrentHashMap<String, AdminClient> clientCache = new ConcurrentHashMap<>();

    public AdminClient create(String bootstrapServers) {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalArgumentException("Bootstrap server adresi boş olamaz");
        }

        // Cache'de varsa direkt dön — yoksa yarat ve cache'e ekle
        return clientCache.computeIfAbsent(bootstrapServers, this::createNewClient);
    }

    private AdminClient createNewClient(String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        try {
            System.out.println("Yeni AdminClient oluşturuluyor: " + bootstrapServers);
            return AdminClient.create(config);
        } catch (Exception e) {
            throw new ClusterConnectionException(
                "Kafka cluster'ına bağlanılamadı: " + bootstrapServers + " — " + e.getMessage()
            );
        }
    }

    // Bağlantı bozulursa cache'den temizle
    public void invalidate(String bootstrapServers) {
        AdminClient client = clientCache.remove(bootstrapServers);
        if (client != null) {
            client.close();
            System.out.println("AdminClient kapatıldı: " + bootstrapServers);
        }
    }

    // Uygulama kapanırken tüm bağlantıları kapat
    @PreDestroy
    public void closeAll() {
        System.out.println("Tüm AdminClient'lar kapatılıyor...");
        clientCache.values().forEach(AdminClient::close);
        clientCache.clear();
    }
}