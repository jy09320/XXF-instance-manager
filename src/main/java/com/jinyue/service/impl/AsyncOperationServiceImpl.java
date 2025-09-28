package com.jinyue.service.impl;

import com.jinyue.dto.OperationResult;
import com.jinyue.entity.TaskInfo;
import com.jinyue.service.IAsyncOperationService;
import com.jinyue.service.ITaskManagerService;
import com.jinyue.service.INapcatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncOperationServiceImpl implements IAsyncOperationService {

    private final ExecutorService operationExecutor = Executors.newFixedThreadPool(10);
    private final INapcatInstanceService instanceService;
    private final ITaskManagerService taskManagerService;

    @Async("taskExecutor")
    @Override
    public void executeStartOperationsAsync(String taskId, List<String> instanceIds) {
        executeOperations(taskId, instanceIds, "START", instanceService::startInstance);
    }

    @Async("taskExecutor")
    @Override
    public void executeStopOperationsAsync(String taskId, List<String> instanceIds) {
        executeOperations(taskId, instanceIds, "STOP", instanceService::stopInstance);
    }

    @Async("taskExecutor")
    @Override
    public void executeRestartOperationsAsync(String taskId, List<String> instanceIds) {
        executeOperations(taskId, instanceIds, "RESTART", instanceService::restartInstance);
    }

    @Async("taskExecutor")
    @Override
    public void executeDeleteOperationsAsync(String taskId, List<String> instanceIds) {
        executeOperations(taskId, instanceIds, "DELETE", instanceService::deleteInstance);
    }

    private void executeOperations(String taskId, List<String> instanceIds,
                                 String operation, OperationHandler handler) {
        try {
            taskManagerService.updateTaskStatus(taskId, TaskInfo.TaskStatus.RUNNING);
            //创建并行任务
            List<CompletableFuture<OperationResult>> futures = instanceIds.stream()
                    .map(instanceId -> CompletableFuture.supplyAsync(() -> {
                        try {
                            handler.execute(instanceId);
                            return new OperationResult(instanceId, true, null);
                        } catch (Exception e) {
                            return new OperationResult(instanceId, false, e.getMessage());
                        }
                    }, operationExecutor)).toList();
            //等待所有任务完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            //设置超时时间
            allOf.get(60, TimeUnit.SECONDS);

            //收集结果
            List<String> successIds =new ArrayList<>();
            List<TaskInfo.FailedOperation> failedOperations = new ArrayList<>();
            for (CompletableFuture<OperationResult> future : futures) {
                OperationResult result = future.get();
                if (result.isSuccess()) {
                    successIds.add(result.getInstanceId());
                } else {
                    TaskInfo.FailedOperation failed = new TaskInfo.FailedOperation();
                    failed.setInstanceId(result.getInstanceId());
                    failed.setReason(result.getErrorMessage());
                    failedOperations.add(failed);
                }
                taskManagerService.completeTask(taskId, successIds, failedOperations);
                log.info("Task {} completed: {} success, {} failed",
                        taskId, successIds.size(), failedOperations.size());
            }
        } catch (Exception e) {
            taskManagerService.failTask(taskId, e.getMessage());
            log.error("Task {} failed: {}", taskId, e.getMessage());
        }

    }

    @FunctionalInterface
    private interface OperationHandler {
        void execute(String instanceId) throws Exception;
    }
}