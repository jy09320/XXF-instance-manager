package com.jinyue.dto;

import com.jinyue.entity.TaskInfo;
import lombok.Data;

// 添加异步响应DTO
@Data
public class AsyncOperationResponse {
    private String taskId;
    private String message;
    private TaskInfo.TaskStatus status;

    public static AsyncOperationResponse of(TaskInfo task) {
        AsyncOperationResponse response = new AsyncOperationResponse();
        response.setTaskId(task.getTaskId());
        response.setStatus(task.getStatus());
        response.setMessage("Operation started, check status with taskId");
        return response;
    }
}