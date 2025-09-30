package com.jinyue.controller;

import com.jinyue.dto.WebhookMessageRequest;
import com.jinyue.dto.WebhookResponse;
import com.jinyue.service.IWebhookMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook接口", description = "接收NapCat推送的消息")
public class WebhookController {

    private final IWebhookMessageService webhookMessageService;

    @PostMapping("/message")
    @Operation(summary = "接收NapCat消息", description = "NapCat通过Webhook推送消息到此接口")
    public ResponseEntity<WebhookResponse> receiveMessage(@Valid @RequestBody WebhookMessageRequest request) {
        try {
            log.info("Received webhook message: messageId={}, userId={}, messageType={}, content={}",
                    request.getMessageId(), request.getUserId(), request.getMessageType(), request.getRawMessage());

            // 验证消息类型
            if (!"message".equals(request.getPostType())) {
                log.warn("Invalid post_type: {}, expected 'message'", request.getPostType());
                return ResponseEntity.badRequest().body(
                        WebhookResponse.error("Invalid post_type, expected 'message'")
                );
            }

            // 验证消息类型
            if (!"group".equals(request.getMessageType()) && !"private".equals(request.getMessageType())) {
                log.warn("Invalid message_type: {}, expected 'group' or 'private'", request.getMessageType());
                return ResponseEntity.badRequest().body(
                        WebhookResponse.error("Invalid message_type, expected 'group' or 'private'")
                );
            }

            // 处理消息
            webhookMessageService.processMessage(request);

            log.info("Successfully processed webhook message: messageId={}", request.getMessageId());
            return ResponseEntity.ok(WebhookResponse.success("消息接收成功"));

        } catch (Exception e) {
            log.error("Failed to process webhook message: messageId={}, error={}",
                    request.getMessageId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    WebhookResponse.error("消息处理失败: " + e.getMessage())
            );
        }
    }
}