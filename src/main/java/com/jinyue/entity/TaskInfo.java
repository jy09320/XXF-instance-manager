package com.jinyue.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class TaskInfo {
    private String taskId;                    // 任务唯一ID
    private String operation;                 // 操作类型: START/STOP/RESTART/DELETE
    private TaskStatus status;                // 任务状态
    private List<String> instanceIds;         // 目标实例ID列表
    private int totalCount;                   // 总数量
    private int successCount;                 // 成功数量
    private int failedCount;                  // 失败数量
    private List<String> successIds;          // 成功实例ID
    private List<FailedOperation> failedOperations; // 失败详情
    private LocalDateTime createdTime;        // 创建时间
    private LocalDateTime completedTime;      // 完成时间
    private String errorMessage;              // 错误信息

    public enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }

    @Data
    public static class FailedOperation {
        private String instanceId;
        private String reason;
    }
}