package com.muratsag.kafkaboard.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.cluster.ClusterRepository;
import com.muratsag.kafkaboard.dto.ConsumerGroupInfoDto;
import com.muratsag.kafkaboard.kafka.KafkaConsumerGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class LagBroadcastScheduler {

    private final LagWebSocketHandler lagWebSocketHandler;
    private final KafkaConsumerGroupService kafkaConsumerGroupService;
    private final ClusterRepository clusterRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 10000)
    public void broadcastLagUpdates() {
        for (Map.Entry<UUID, CopyOnWriteArrayList<WebSocketSession>> entry : lagWebSocketHandler.getSessionsByCluster().entrySet()) {
            UUID clusterId = entry.getKey();
            List<WebSocketSession> sessions = entry.getValue();

            if (sessions.isEmpty()) {
                continue;
            }

            boolean hasOpenSession = sessions.stream().anyMatch(WebSocketSession::isOpen);
            if (!hasOpenSession) {
                sessions.forEach(lagWebSocketHandler::removeSession);
                continue;
            }

            Optional<ClusterEntity> clusterOpt = clusterRepository.findById(clusterId);
            if (clusterOpt.isEmpty()) {
                sendError(sessions, "Cluster bulunamadı: " + clusterId);
                continue;
            }

            try {
                List<ConsumerGroupInfoDto> lagData = kafkaConsumerGroupService.getConsumerGroupLag(clusterOpt.get());
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
