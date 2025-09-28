package com.jinyue.service.impl;

import com.jinyue.entity.TaskInfo;
import com.jinyue.service.ITaskManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TaskManagerServiceImpl implements ITaskManagerService {

    // 内存存储，后续可改为Redis
    private final ConcurrentHashMap<String, TaskInfo> tasks = new ConcurrentHashMap<>();

    @Override
    public TaskInfo createTask(String operation, List<String> instanceIds) {
        String taskId = UUID.randomUUID().toString();

        TaskInfo task = new TaskInfo()
                .setTaskId(taskId)
                .setOperation(operation)
                .setStatus(TaskInfo.TaskStatus.PENDING)
                .setInstanceIds(new ArrayList<>(instanceIds))
                .setTotalCount(instanceIds.size())
                .setSuccessCount(0)
                .setFailedCount(0)
                .setSuccessIds(new ArrayList<>())
                .setFailedOperations(new ArrayList<>())
                .setCreatedTime(LocalDateTime.now());

        tasks.put(taskId, task);
        log.info("Created task: {} for operation: {} on {} instances",
                taskId, operation, instanceIds.size());

        return task;
    }

    @Override
    public TaskInfo getTask(String taskId) {
        return tasks.get(taskId);
    }

    @Override
    public void updateTaskStatus(String taskId, TaskInfo.TaskStatus status) {
        TaskInfo task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            if (status == TaskInfo.TaskStatus.COMPLETED || status == TaskInfo.TaskStatus.FAILED) {
                task.setCompletedTime(LocalDateTime.now());
            }
        }
    }

    @Override
    public void updateTaskProgress(String taskId, int successCount, int failedCount) {
        TaskInfo task = tasks.get(taskId);
        if (task != null) {
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
        }
    }

    @Override
    public void completeTask(String taskId, List<String> successIds,
                           List<TaskInfo.FailedOperation> failedOperations) {
        TaskInfo task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskInfo.TaskStatus.COMPLETED)
                .setSuccessIds(new ArrayList<>(successIds))
                .setFailedOperations(new ArrayList<>(failedOperations))
                .setSuccessCount(successIds.size())
                .setFailedCount(failedOperations.size())
                .setCompletedTime(LocalDateTime.now());
        }
    }

    @Override
    public void failTask(String taskId, String errorMessage) {
        TaskInfo task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskInfo.TaskStatus.FAILED)
                .setErrorMessage(errorMessage)
                .setCompletedTime(LocalDateTime.now());
        }
    }

    @Override
    public void cleanupExpiredTasks() {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
        tasks.entrySet().removeIf(entry ->
            entry.getValue().getCreatedTime().isBefore(expireTime));
    }
}