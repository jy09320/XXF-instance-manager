package com.jinyue.controller;

import com.jinyue.entity.TaskInfo;
import com.jinyue.mapper.NapcatInstanceMapper;
import com.jinyue.service.ITaskManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Napcat异步任务管理", description = "管理Napcat实例的异步任务")
public class TaskController {

    private final ITaskManagerService taskManagerService;

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询任务状态", description = "根据taskId查询异步任务的执行状态")
    public ResponseEntity<TaskInfo> getTaskStatus(@PathVariable String taskId) {
        try {
            TaskInfo task = taskManagerService.getTask(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            log.error("Failed to get task status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
