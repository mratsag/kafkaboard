package com.muratsag.kafkaboard.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratsag.kafkaboard.dto.ConsumerGroupInfoDto;
import com.muratsag.kafkaboard.kafka.KafkaConsumerGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class LagBroadcastScheduler {

    private final LagWebSocketHandler lagWebSocketHandler;
    private final KafkaConsumerGroupService kafkaConsumerGroupService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 10000)
    public void broadcastLagUpdates() {
        for (Map.Entry<UUID, CopyOnWriteArrayList<WebSocketSession>> entry : lagWebSocketHandler.getSessionsByCluster().entrySet()) {
            UUID clusterId = entry.getKey();
            List<WebSocketSession> sessions = entry.getValue();

            if (sessions.isEmpty()) {
                continue;
            }

            WebSocketSession sampleSession = sessions.stream()
                    .filter(WebSocketSession::isOpen)
                    .findFirst()
                    .orElse(null);

            if (sampleSession == null) {
                sessions.forEach(lagWebSocketHandler::removeSession);
                continue;
            }

            String bootstrapServers = (String) sampleSession.getAttributes().get("bootstrapServers");
            if (bootstrapServers == null || bootstrapServers.isBlank()) {
                sendError(sessions, "Cluster bağlantı bilgisi bulunamadı");
                continue;
            }

            try {
                List<ConsumerGroupInfoDto> lagData = kafkaConsumerGroupService.getConsumerGroupLag(bootstrapServers);
                String payload = objectMapper.writeValueAsString(lagData);
                sessions.forEach(session -> lagWebSocketHandler.sendMessage(session, payload));
            } catch (Exception e) {
                log.debug("Lag broadcast failed for cluster {}: {}", clusterId, e.getMessage());
                sendError(sessions, e.getMessage());
            }
        }
    }

    private void sendError(List<WebSocketSession> sessions, String message) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("error", message));
            sessions.forEach(session -> lagWebSocketHandler.sendMessage(session, payload));
        } catch (JsonProcessingException e) {
            log.debug("Lag error payload serialization failed: {}", e.getMessage());
        }
    }
}
