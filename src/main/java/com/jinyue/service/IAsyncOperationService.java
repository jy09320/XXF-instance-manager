package com.jinyue.service;

import java.util.List;

public interface IAsyncOperationService {

    /**
     * 异步执行启动操作
     */
    void executeStartOperationsAsync(String taskId, List<String> instanceIds);

    /**
     * 异步执行停止操作
     */
    void executeStopOperationsAsync(String taskId, List<String> instanceIds);

    /**
     * 异步执行重启操作
     */
    void executeRestartOperationsAsync(String taskId, List<String> instanceIds);

    /**
     * 异步执行删除操作
     */
    void executeDeleteOperationsAsync(String taskId, List<String> instanceIds);
}