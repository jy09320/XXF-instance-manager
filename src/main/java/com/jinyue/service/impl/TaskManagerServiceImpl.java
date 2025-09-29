package com.jinyue.service.impl;

import com.jinyue.entity.NapcatInstance;
import com.jinyue.entity.TaskInfo;
import com.jinyue.exception.InvalidInstanceStateException;
import com.jinyue.service.INapcatInstanceService;
import com.jinyue.service.ITaskManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskManagerServiceImpl implements ITaskManagerService {

    // 内存存储，后续可改为Redis
    private final ConcurrentHashMap<String, TaskInfo> tasks = new ConcurrentHashMap<>();

    private final INapcatInstanceService instanceService;

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

    @Override
    public TaskInfo createTaskWithValidation(String operation, List<String> instanceIds) {
        // 验证实例状态
        validateInstanceStatesForOperation(operation, instanceIds);

        // 如果验证通过，创建任务
        return createTask(operation, instanceIds);
    }

    private void validateInstanceStatesForOperation(String operation, List<String> instanceIds) {
        List<InvalidInstanceStateException.InvalidStateInfo> invalidStates = new ArrayList<>();

        for (String instanceId : instanceIds) {
            try {
                // 获取实例当前状态
                var instanceResponse = instanceService.getInstance(instanceId);
                NapcatInstance.InstanceStatus currentStatus =
                    NapcatInstance.InstanceStatus.valueOf(String.valueOf(instanceResponse.getStatus()));

                // 根据操作类型检查状态是否合适
                String validationResult = validateStateForOperation(operation, currentStatus);

                if (validationResult != null) {
                    invalidStates.add(new InvalidInstanceStateException.InvalidStateInfo(
                        instanceId,
                        currentStatus.toString(),
                        getRequiredStateForOperation(operation),
                        validationResult
                    ));
                }
            } catch (RuntimeException e) {
                // 实例不存在或获取失败
                invalidStates.add(new InvalidInstanceStateException.InvalidStateInfo(
                    instanceId,
                    "UNKNOWN",
                    getRequiredStateForOperation(operation),
                    "实例不存在或无法获取状态: " + e.getMessage()
                ));
            }
        }

        if (!invalidStates.isEmpty()) {
            throw new InvalidInstanceStateException(
                String.format("无法执行 %s 操作，有 %d 个实例状态不符合要求",
                    operation, invalidStates.size()),
                invalidStates
            );
        }
    }

    private String validateStateForOperation(String operation, NapcatInstance.InstanceStatus currentStatus) {
        switch (operation.toUpperCase()) {
            case "START":
                if (currentStatus == NapcatInstance.InstanceStatus.RUNNING) {
                    return "实例已经在运行中";
                }
                if (currentStatus == NapcatInstance.InstanceStatus.STARTING) {
                    return "实例正在启动中";
                }
                break;
            case "STOP":
                if (currentStatus == NapcatInstance.InstanceStatus.STOPPED) {
                    return "实例已经停止";
                }
                if (currentStatus == NapcatInstance.InstanceStatus.STOPPING) {
                    return "实例正在停止中";
                }
                break;
            case "RESTART":
                if (currentStatus == NapcatInstance.InstanceStatus.STARTING ||
                    currentStatus == NapcatInstance.InstanceStatus.STOPPING) {
                    return "实例正在进行状态转换，无法重启";
                }
                break;
            case "DELETE":
                if (currentStatus == NapcatInstance.InstanceStatus.STARTING ||
                    currentStatus == NapcatInstance.InstanceStatus.STOPPING) {
                    return "实例正在进行状态转换，无法删除";
                }
                break;
            default:
                return "不支持的操作类型: " + operation;
        }
        return null; // 状态检查通过
    }

    private String getRequiredStateForOperation(String operation) {
        return switch (operation.toUpperCase()) {
            case "START" -> "STOPPED";
            case "STOP" -> "RUNNING";
            case "RESTART" -> "RUNNING or STOPPED";
            case "DELETE" -> "STOPPED or RUNNING";
            default -> "UNKNOWN";
        };
    }
}