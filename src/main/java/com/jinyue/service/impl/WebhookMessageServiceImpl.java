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

    @Value("${message.forward.target-url:}")
    private String targetUrl;

    @Override
    public void processMessage(WebhookMessageRequest request) {
        try {
            // 1. 查找实例ID
            String qqAccount = request.getSelfId().toString();
            NapcatInstance instance = napcatInstanceService.lambdaQuery()
                    .eq(NapcatInstance::getQqAccount, qqAccount)
                    .one();

            if (instance == null) {
                log.warn("No instance found for selfId: {}", request.getSelfId());
                return;
            }

            String instanceId = instance.getId();
            log.debug("Found instanceId {} for selfId {}", instanceId, request.getSelfId());

            // 2. 转发消息（如果配置了目标URL）
            if (targetUrl != null && !targetUrl.isEmpty()) {
                forwardMessage(request, instanceId);
            }

        } catch (Exception e) {
            log.error("Error processing message: messageId={}, error={}",
                    request.getMessageId(), e.getMessage(), e);
        }
    }

    /**
     * 转发消息到外部服务
     */
    private void forwardMessage(WebhookMessageRequest request, String instanceId) {
        try {
            // 创建简单的转发数据
            Map<String, Object> data = new HashMap<>();
            data.put("instanceId", instanceId);
            data.put("postType", request.getPostType());
            data.put("messageType", request.getMessageType());
            data.put("messageId", request.getMessageId());
            data.put("userId", request.getUserId());
            data.put("selfId", request.getSelfId());
            data.put("rawMessage", request.getRawMessage());
            data.put("time", request.getTime());
            data.put("sender", request.getSender());

            // 添加群组信息（如果是群消息）
            if (request.getGroupId() != null) {
                data.put("groupId", request.getGroupId());
                data.put("groupName", request.getGroupName());
            }

            // 直接转发
            restTemplate.postForObject(targetUrl, data, String.class);
            log.info("Message forwarded: messageId={}, instanceId={}", request.getMessageId(), instanceId);

        } catch (Exception e) {
            log.error("Forward failed: messageId={}, instanceId={}, error={}",
                    request.getMessageId(), instanceId, e.getMessage());
        }
    }
}