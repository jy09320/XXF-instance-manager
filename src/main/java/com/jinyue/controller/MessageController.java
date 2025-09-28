package com.jinyue.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jinyue.entity.MessageLog;
import com.jinyue.service.IMessageLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "消息管理", description = "管理Napcat实例的消息日志")
public class MessageController {

    private final IMessageLogService messageLogService;

    @GetMapping
    @Operation(summary = "获取所有消息", description = "分页获取所有消息日志")
    public ResponseEntity<IPage<MessageLog>> getAllMessages(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        try {
            IPage<MessageLog> messages = messageLogService.getAllMessages(page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Failed to get messages: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{instanceId}")
    @Operation(summary = "获取实例消息", description = "分页获取指定实例的消息日志")
    public ResponseEntity<IPage<MessageLog>> getMessagesByInstance(
            @Parameter(description = "实例ID") @PathVariable String instanceId,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        try {
            IPage<MessageLog> messages = messageLogService.getMessagesByInstance(instanceId, page, size);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Failed to get messages for instance {}: {}", instanceId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}