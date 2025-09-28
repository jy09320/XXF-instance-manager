package com.jinyue.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface IMessageForwardingService {

    /**
     * 异步转发消息
     * @param messageId 消息ID
     * @param messageContent 消息内容
     * @return 异步结果
     */
    CompletableFuture<Void> forwardMessage(String messageId, Map<String, Object> messageContent);
}