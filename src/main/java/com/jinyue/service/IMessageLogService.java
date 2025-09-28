package com.jinyue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinyue.entity.MessageLog;

import java.util.Map;

public interface IMessageLogService extends IService<MessageLog> {

    /**
     * 记录消息日志
     * @param instanceId 实例ID
     * @param direction 消息方向
     * @param messageType 消息类型
     * @param content 消息内容
     * @return 消息日志记录
     */
    MessageLog logMessage(String instanceId, MessageLog.MessageDirection direction,
                         String messageType, Map<String, Object> content);

    /**
     * 标记消息为已转发
     * @param messageId 消息ID
     */
    void markAsForwarded(String messageId);

    /**
     * 分页获取指定实例的消息
     * @param instanceId 实例ID
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    IPage<MessageLog> getMessagesByInstance(String instanceId, int page, int size);

    /**
     * 分页获取所有消息
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    IPage<MessageLog> getAllMessages(int page, int size);
}