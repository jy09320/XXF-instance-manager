package com.jinyue.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jinyue.dto.*;
import com.jinyue.entity.TaskInfo;
import com.jinyue.exception.InvalidInstanceStateException;
import com.jinyue.service.IAsyncOperationService;
import com.jinyue.service.INapcatInstanceService;
import com.jinyue.service.ITaskManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Tag(name = "Napcat实例管理", description = "管理Napcat实例的生命周期")
public class NapcatInstanceController {
    // 注入新的服务
    private final ITaskManagerService taskManagerService;
    private final IAsyncOperationService asyncOperationService;

    private final INapcatInstanceService instanceService;


    @PostMapping
    @Operation(summary = "创建Napcat实例", description = "创建单个或多个Napcat实例，自动分配端口号")
    public ResponseEntity<MultiInstanceResponse> createMultipleInstances(@Valid @RequestBody CreateMultiInstanceRequest request) {
        try {
            MultiInstanceResponse response = instanceService.createMultipleInstances(request);

            if (response.getFailedCount() > 0 && response.getSuccessCount() == 0) {
                return ResponseEntity.badRequest().body(response);
            } else if (response.getFailedCount() > 0) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid batch create request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to create multiple instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    @Operation(summary = "获取实例列表", description = "分页获取所有Napcat实例")
    public ResponseEntity<IPage<InstanceResponse>> getInstances(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        try {
            IPage<InstanceResponse> instances = instanceService.getInstances(page, size);
            return ResponseEntity.ok(instances);
        } catch (Exception e) {
            log.error("Failed to get instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有实例", description = "获取所有Napcat实例（不分页）")
    public ResponseEntity<List<InstanceResponse>> getAllInstances() {
        try {
            List<InstanceResponse> instances = instanceService.getAllInstances();
            return ResponseEntity.ok(instances);
        } catch (Exception e) {
            log.error("Failed to get all instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取实例详情", description = "根据ID获取Napcat实例详情")
    public ResponseEntity<InstanceResponse> getInstance(
            @Parameter(description = "实例ID") @PathVariable String id) {
        try {
            InstanceResponse instance = instanceService.getInstance(id);
            return ResponseEntity.ok(instance);
        } catch (RuntimeException e) {
            log.warn("Instance not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get instance {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/start")
    @Operation(summary = "启动实例", description = "异步启动单个或多个Napcat实例")
    public ResponseEntity<?> startInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            // 创建任务（包含状态验证）
            TaskInfo task = taskManagerService.createTaskWithValidation("START", request.getIds());

            // 异步执行
            asyncOperationService.executeStartOperationsAsync(task.getTaskId(), request.getIds());

            // 立即返回任务ID
            return ResponseEntity.accepted().body(AsyncOperationResponse.of(task));

        } catch (InvalidInstanceStateException e) {
            log.warn("Invalid instance states for start operation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "invalidStates", e.getInvalidStates()
            ));
        } catch (Exception e) {
            log.error("Failed to start instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to start operation"));
        }
    }

// 同样修改stop、restart、delete方法，调用对应的async方法

    @PutMapping("/stop")
    @Operation(summary = "停止实例", description = "异步停止单个或多个Napcat实例")
    public ResponseEntity<?> stopInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            // 创建任务（包含状态验证）
            TaskInfo task = taskManagerService.createTaskWithValidation("STOP", request.getIds());

            // 异步执行
            asyncOperationService.executeStopOperationsAsync(task.getTaskId(), request.getIds());

            // 立即返回任务ID
            return ResponseEntity.accepted().body(AsyncOperationResponse.of(task));

        } catch (InvalidInstanceStateException e) {
            log.warn("Invalid instance states for stop operation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "invalidStates", e.getInvalidStates()
            ));
        } catch (Exception e) {
            log.error("Failed to stop instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/restart")
    @Operation(summary = "重启实例", description = "重启单个或多个Napcat实例")
    public ResponseEntity<?> restartInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            // 创建任务（包含状态验证）
            TaskInfo task = taskManagerService.createTaskWithValidation("RESTART", request.getIds());

            // 异步执行
            asyncOperationService.executeRestartOperationsAsync(task.getTaskId(), request.getIds());

            // 立即返回任务ID
            return ResponseEntity.accepted().body(AsyncOperationResponse.of(task));

        } catch (InvalidInstanceStateException e) {
            log.warn("Invalid instance states for restart operation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "invalidStates", e.getInvalidStates()
            ));
        } catch (Exception e) {
            log.error("Failed to restart instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @DeleteMapping("")
    @Operation(summary = "删除实例", description = "删除单个或多个Napcat实例")
    public ResponseEntity<?> deleteInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            // 创建任务（包含状态验证）
            TaskInfo task = taskManagerService.createTaskWithValidation("DELETE", request.getIds());

            // 异步执行
            asyncOperationService.executeDeleteOperationsAsync(task.getTaskId(), request.getIds());

            // 立即返回任务ID
            return ResponseEntity.accepted().body(AsyncOperationResponse.of(task));

        } catch (InvalidInstanceStateException e) {
            log.warn("Invalid instance states for delete operation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "invalidStates", e.getInvalidStates()
            ));
        } catch (Exception e) {
            log.error("Failed to delete instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

}