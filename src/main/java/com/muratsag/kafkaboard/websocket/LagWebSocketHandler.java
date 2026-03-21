package com.muratsag.kafkaboard.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class LagWebSocketHandler extends TextWebSocketHandler {

    private final Map<UUID, CopyOnWriteArrayList<WebSocketSession>> sessionsByCluster = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID clusterId = (UUID) session.getAttributes().get("clusterId");
        if (clusterId == null) {
            closeSilently(session, CloseStatus.BAD_DATA);
            return;
        }

        sessionsByCluster
                .computeIfAbsent(clusterId, ignored -> new CopyOnWriteArrayList<>())
                .add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        removeSession(session);
        closeSilently(session, CloseStatus.SERVER_ERROR);
    }

    public Map<UUID, CopyOnWriteArrayList<WebSocketSession>> getSessionsByCluster() {
        return sessionsByCluster;
    }

    public void removeSession(WebSocketSession session) {
        UUID clusterId = (UUID) session.getAttributes().get("clusterId");
        if (clusterId == null) {
            return;
        }

        List<WebSocketSession> sessions = sessionsByCluster.get(clusterId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByCluster.remove(clusterId, (CopyOnWriteArrayList<WebSocketSession>) sessions);
        }
    }

    public void sendMessage(WebSocketSession session, String payload) {
        try {
            if (!session.isOpen()) {
                removeSession(session);
                return;
            }

            session.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            log.debug("WebSocket message send failed: {}", e.getMessage());
            removeSession(session);
            closeSilently(session, CloseStatus.SERVER_ERROR);
        }
    }

    private void closeSilently(WebSocketSession session, CloseStatus closeStatus) {
        try {
            if (session.isOpen()) {
                session.close(closeStatus);
            }
        } catch (IOException ignored) {
            // Ignore close failures on best-effort cleanup.
        }
    }
}
