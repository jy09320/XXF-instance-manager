package com.jinyue.service;

import com.jinyue.entity.TaskInfo;
import java.util.List;

public interface ITaskManagerService {

    /**
     * 创建新任务
     */
    TaskInfo createTask(String operation, List<String> instanceIds);

    /**
     * 获取任务状态
     */
    TaskInfo getTask(String taskId);

    /**
     * 更新任务状态
     */
    void updateTaskStatus(String taskId, TaskInfo.TaskStatus status);

    /**
     * 更新任务进度
     */
    void updateTaskProgress(String taskId, int successCount, int failedCount);

    /**
     * 标记任务完成
     */
    void completeTask(String taskId, List<String> successIds,
                     List<TaskInfo.FailedOperation> failedOperations);

    /**
     * 标记任务失败
     */
    void failTask(String taskId, String errorMessage);

    /**
     * 清理过期任务
     */
    void cleanupExpiredTasks();
}