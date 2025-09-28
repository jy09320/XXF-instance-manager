package com.jinyue.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinyue.service.IMessageForwardingService;
import com.jinyue.service.IMessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageForwardingServiceImpl implements IMessageForwardingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final IMessageLogService messageLogService;

    @Value("${napcat.forwarding.enabled:true}")
    private boolean forwardingEnabled;

    @Value("${napcat.forwarding.target-url}")
    private String targetUrl;

    @Value("${napcat.forwarding.retry-attempts:3}")
    private int retryAttempts;

    @Value("${napcat.forwarding.timeout:30000}")
    private int timeout;

    @Override
    public CompletableFuture<Void> forwardMessage(String messageId, Map<String, Object> messageContent) {
        if (!forwardingEnabled) {
            log.debug("Message forwarding is disabled");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                boolean success = forwardWithRetry(messageContent);
                if (success) {
                    messageLogService.markAsForwarded(messageId);
                    log.debug("Successfully forwarded message {}", messageId);
                } else {
                    log.error("Failed to forward message {} after {} attempts", messageId, retryAttempts);
                }
            } catch (Exception e) {
                log.error("Error forwarding message {}: {}", messageId, e.getMessage());
            }
        });
    }

    private boolean forwardWithRetry(Map<String, Object> messageContent) {
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(messageContent, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        targetUrl, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }

                log.warn("Forward attempt {} failed with status: {}",
                        attempt, response.getStatusCode());

            } catch (Exception e) {
                log.warn("Forward attempt {} failed: {}", attempt, e.getMessage());
            }

            if (attempt < retryAttempts) {
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }
}