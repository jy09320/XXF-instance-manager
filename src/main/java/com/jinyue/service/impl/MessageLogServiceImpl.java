package com.jinyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyue.entity.MessageLog;
import com.jinyue.mapper.MessageLogMapper;
import com.jinyue.service.IMessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLogServiceImpl extends ServiceImpl<MessageLogMapper, MessageLog>
        implements IMessageLogService {

    @Override
    public MessageLog logMessage(String instanceId, MessageLog.MessageDirection direction,
                                String messageType, Map<String, Object> content) {
        MessageLog messageLog = new MessageLog()
                .setInstanceId(instanceId)
                .setDirection(direction)
                .setMessageType(messageType)
                .setContent(content)
                .setForwarded(false)
                .setCreatedTime(LocalDateTime.now());

        if (save(messageLog)) {
            log.debug("Logged message for instance {}: {} - {}", instanceId, direction, messageType);
            return messageLog;
        } else {
            log.error("Failed to log message for instance {}", instanceId);
            throw new RuntimeException("Failed to log message");
        }
    }

    @Override
    public void markAsForwarded(String messageId) {
        MessageLog messageLog = getById(messageId);
        if (messageLog != null) {
            messageLog.setForwarded(true);
            updateById(messageLog);
        }
    }

    @Override
    public IPage<MessageLog> getMessagesByInstance(String instanceId, int page, int size) {
        Page<MessageLog> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<MessageLog> queryWrapper = new LambdaQueryWrapper<MessageLog>()
                .eq(MessageLog::getInstanceId, instanceId)
                .orderByDesc(MessageLog::getCreatedTime);

        return page(pageRequest, queryWrapper);
    }

    @Override
    public IPage<MessageLog> getAllMessages(int page, int size) {
        Page<MessageLog> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<MessageLog> queryWrapper = new LambdaQueryWrapper<MessageLog>()
                .orderByDesc(MessageLog::getCreatedTime);

        return page(pageRequest, queryWrapper);
    }
}