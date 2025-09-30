package com.jinyue.service;

import com.jinyue.dto.WebhookMessageRequest;

public interface IWebhookMessageService {

    /**
     * 处理接收到的Webhook消息
     * 统一处理群组和私聊消息，添加实例ID并转发
     * @param request 消息请求
     */
    void processMessage(WebhookMessageRequest request);
}