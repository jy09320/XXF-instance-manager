package com.jinyue.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinyue.dto.MessageDto;
import com.jinyue.entity.MessageLog;
import com.jinyue.entity.NapcatInstance;
import com.jinyue.service.IMessageForwardingService;
import com.jinyue.service.IMessageLogService;
import com.jinyue.service.INapcatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NapcatWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final INapcatInstanceService instanceService;
    private final IMessageLogService messageLogService;
    private final IMessageForwardingService messageForwardingService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionInstanceMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String instanceId = extractInstanceIdFromPath(session);
        if (instanceId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid instance ID"));
            return;
        }

        try {
            NapcatInstance instance = instanceService.getById(instanceId);
            if (instance == null) {
                session.close(CloseStatus.NO_STATUS_CODE.withReason("Instance not found"));
                return;
            }

            sessions.put(session.getId(), session);
            sessionInstanceMap.put(session.getId(), instanceId);

            log.info("Napcat instance {} connected via WebSocket: {}", instanceId, session.getId());

        } catch (Exception e) {
            log.error("Error connecting instance {}: {}", instanceId, e.getMessage());
            session.close(CloseStatus.SERVER_ERROR.withReason("Connection error"));
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (!(message instanceof TextMessage)) {
            return;
        }

        String instanceId = sessionInstanceMap.get(session.getId());
        if (instanceId == null) {
            log.warn("Received message from unknown session: {}", session.getId());
            return;
        }

        try {
            String payload = ((TextMessage) message).getPayload();
            Map<String, Object> messageContent = objectMapper.readValue(payload, Map.class);

            MessageLog messageLog = messageLogService.logMessage(
                    instanceId,
                    MessageLog.MessageDirection.INBOUND,
                    extractMessageType(messageContent),
                    messageContent
            );

            messageForwardingService.forwardMessage(messageLog.getId(), messageContent);

            log.debug("Processed message from instance {}: {}", instanceId, messageContent.get("type"));

        } catch (Exception e) {
            log.error("Error processing message from instance {}: {}", instanceId, e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String instanceId = sessionInstanceMap.get(session.getId());
        log.error("WebSocket transport error for instance {}: {}", instanceId, exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String instanceId = sessionInstanceMap.remove(session.getId());
        sessions.remove(session.getId());

        log.info("Napcat instance {} disconnected: {} - {}", instanceId, closeStatus.getCode(), closeStatus.getReason());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void sendMessageToInstance(String instanceId, Map<String, Object> message) {
        WebSocketSession session = findSessionByInstanceId(instanceId);
        if (session != null && session.isOpen()) {
            try {
                String payload = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(payload));

                messageLogService.logMessage(
                        instanceId,
                        MessageLog.MessageDirection.OUTBOUND,
                        extractMessageType(message),
                        message
                );

                log.debug("Sent message to instance {}: {}", instanceId, message.get("type"));

            } catch (Exception e) {
                log.error("Failed to send message to instance {}: {}", instanceId, e.getMessage());
            }
        } else {
            log.warn("No active session found for instance: {}", instanceId);
        }
    }

    public boolean isInstanceConnected(String instanceId) {
        WebSocketSession session = findSessionByInstanceId(instanceId);
        return session != null && session.isOpen();
    }

    private String extractInstanceIdFromPath(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null) {
            String path = uri.getPath();
            String[] segments = path.split("/");
            if (segments.length >= 3 && "napcat".equals(segments[segments.length - 2])) {
                return segments[segments.length - 1];
            }
        }
        return null;
    }

    private WebSocketSession findSessionByInstanceId(String instanceId) {
        return sessionInstanceMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(instanceId))
                .map(Map.Entry::getKey)
                .findFirst()
                .map(sessions::get)
                .orElse(null);
    }

    private String extractMessageType(Map<String, Object> message) {
        Object type = message.get("type");
        return type != null ? type.toString() : "unknown";
    }
}