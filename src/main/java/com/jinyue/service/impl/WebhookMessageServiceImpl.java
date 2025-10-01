package com.jinyue.service.impl;

import com.jinyue.dto.WebhookMessageRequest;
import com.jinyue.entity.NapcatInstance;
import com.jinyue.service.INapcatInstanceService;
import com.jinyue.service.IWebhookMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookMessageServiceImpl implements IWebhookMessageService {

    private final INapcatInstanceService napcatInstanceService;
    private final RestTemplate restTemplate;

    @Value("${message.forward.target-url:http://xxf-proxy:8084/api/webhook/napcat-message}")
    private String targetUrl;

    @Override
    public void processMessage(WebhookMessageRequest request) {
        try {
            // 验证消息来源的QQ账号存在
            String qqAccount = request.getSelfId().toString();
            NapcatInstance instance = napcatInstanceService.lambdaQuery()
                    .eq(NapcatInstance::getQqAccount, qqAccount)
                    .one();

            if (instance == null) {
                log.warn("No instance found for selfId: {}", request.getSelfId());
                return;
            }

            log.debug("Processing message from QQ account: {}", qqAccount);

            // 转发消息（如果配置了目标URL）
            if (targetUrl != null && !targetUrl.isEmpty()) {
                forwardMessage(request);
            }

        } catch (Exception e) {
            log.error("Error processing message: messageId={}, error={}",
                    request.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * 转发消息到xxf-proxy (统一使用snake_case命名格式)
     * 架构简化: 不再传递UUID，后端通过QQ号识别账号
     */
    private void forwardMessage(WebhookMessageRequest request) {
        try {
            // 创建符合xxf-bot-backend IncomingMessage格式的数据
            Map<String, Object> data = new HashMap<>();

            // 平台和消息基本信息
            data.put("platform", "qq");
            data.put("message_id", request.getMessageId().toString());
            data.put("message_type", request.getMessageType());

            // 发送者信息
            data.put("sender_id", request.getUserId().toString());
            data.put("sender_nickname", request.getSender().getNickname());

            // 接收者信息 (机器人自己的QQ号，后端通过此字段识别账号)
            data.put("receiver_id", request.getSelfId().toString());

            // 消息内容
            data.put("content", request.getRawMessage());

            // 时间戳 (转换为ISO 8601格式)
            data.put("timestamp", java.time.Instant.ofEpochSecond(request.getTime()).toString());

            // 目标信息 (群聊或私聊)
            if ("group".equals(request.getMessageType())) {
                data.put("target_id", request.getGroupId() != null ? request.getGroupId().toString() : "");
                data.put("target_name", request.getGroupName() != null ? request.getGroupName() : "");
            } else {
                // 私聊时target_id就是发送者的QQ号
                data.put("target_id", request.getUserId().toString());
                data.put("target_name", request.getSender().getNickname());
            }

            // 原始数据 (保留完整的NapCat消息结构)
            Map<String, Object> rawData = new HashMap<>();
            rawData.put("original_napcat_data", request);
            data.put("raw_data", rawData);

            // 转发到xxf-proxy
            restTemplate.postForObject(targetUrl, data, String.class);
            log.info("Message forwarded to xxf-proxy: messageId={}, qqAccount={}, messageType={}",
                    request.getMessageId(), request.getSelfId(), request.getMessageType());

        } catch (Exception e) {
            log.error("Forward to xxf-proxy failed: messageId={}, qqAccount={}, error={}",
                    request.getMessageId(), request.getSelfId(), e.getMessage(), e);
        }
    }
}