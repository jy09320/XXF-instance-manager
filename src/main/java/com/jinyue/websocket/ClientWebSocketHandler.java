package com.jinyue.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    private final NapcatWebSocketHandler napcatHandler;

    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        clientSessions.put(session.getId(), session);
        log.info("Client connected via WebSocket: {}", session.getId());

        sendWelcomeMessage(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (!(message instanceof TextMessage)) {
            return;
        }

        try {
            String payload = ((TextMessage) message).getPayload();
            Map<String, Object> messageContent = objectMapper.readValue(payload, Map.class);

            String action = (String) messageContent.get("action");
            if (action == null) {
                sendErrorMessage(session, "Missing action field");
                return;
            }

            switch (action) {
                case "send_to_instance":
                    handleSendToInstance(session, messageContent);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    sendErrorMessage(session, "Unknown action: " + action);
            }

        } catch (Exception e) {
            log.error("Error processing client message: {}", e.getMessage());
            sendErrorMessage(session, "Invalid message format");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for client {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        clientSessions.remove(session.getId());
        log.info("Client disconnected: {} - {}", closeStatus.getCode(), closeStatus.getReason());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public void broadcastToClients(Map<String, Object> message) {
        clientSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    String payload = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception e) {
                    log.error("Failed to broadcast message to client {}: {}", session.getId(), e.getMessage());
                }
            }
        });
    }

    private void handleSendToInstance(WebSocketSession session, Map<String, Object> messageContent) {
        try {
            Object instanceIdObj = messageContent.get("instanceId");
            Object dataObj = messageContent.get("data");

            if (instanceIdObj == null || dataObj == null) {
                sendErrorMessage(session, "Missing instanceId or data field");
                return;
            }

            String instanceId = instanceIdObj.toString();
            Map<String, Object> data = (Map<String, Object>) dataObj;

            napcatHandler.sendMessageToInstance(instanceId, data);

            sendSuccessMessage(session, "Message sent to instance " + instanceId);
        } catch (Exception e) {
            sendErrorMessage(session, "Failed to send message: " + e.getMessage());
        }
    }

    private void handlePing(WebSocketSession session) {
        try {
            Map<String, Object> pongMessage = Map.of(
                    "type", "pong",
                    "timestamp", System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(pongMessage);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Failed to send pong to client {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendWelcomeMessage(WebSocketSession session) {
        try {
            Map<String, Object> welcomeMessage = Map.of(
                    "type", "welcome",
                    "message", "Connected to Napcat Instance Manager",
                    "timestamp", System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(welcomeMessage);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Failed to send welcome message to client {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendSuccessMessage(WebSocketSession session, String message) {
        try {
            Map<String, Object> successMessage = Map.of(
                    "type", "success",
                    "message", message,
                    "timestamp", System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(successMessage);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Failed to send success message to client {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendErrorMessage(WebSocketSession session, String error) {
        try {
            Map<String, Object> errorMessage = Map.of(
                    "type", "error",
                    "message", error,
                    "timestamp", System.currentTimeMillis()
            );
            String payload = objectMapper.writeValueAsString(errorMessage);
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Failed to send error message to client {}: {}", session.getId(), e.getMessage());
        }
    }
}