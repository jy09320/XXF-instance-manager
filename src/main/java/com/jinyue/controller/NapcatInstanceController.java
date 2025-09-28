package com.jinyue.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jinyue.dto.InstanceOperationRequest;
import com.jinyue.dto.InstanceOperationResponse;
import com.jinyue.dto.CreateMultiInstanceRequest;
import com.jinyue.dto.InstanceResponse;
import com.jinyue.dto.MultiInstanceResponse;
import com.jinyue.service.INapcatInstanceService;
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
    @Operation(summary = "启动实例", description = "启动单个或多个Napcat实例")
    public ResponseEntity<?> startInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            InstanceOperationResponse response = instanceService.startInstances(request);

            // 单个实例时返回简单响应，多个实例时返回详细响应
            if (request.getIds().size() == 1 && response.getFailedCount() == 0) {
                return ResponseEntity.ok(Map.of("message", "Instance started successfully"));
            }

            if (response.getFailedCount() > 0 && response.getSuccessCount() == 0) {
                return ResponseEntity.badRequest().body(response);
            } else if (response.getFailedCount() > 0) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to start instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/stop")
    @Operation(summary = "停止实例", description = "停止单个或多个Napcat实例")
    public ResponseEntity<?> stopInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            InstanceOperationResponse response = instanceService.stopInstances(request);

            // 单个实例时返回简单响应，多个实例时返回详细响应
            if (request.getIds().size() == 1 && response.getFailedCount() == 0) {
                return ResponseEntity.ok(Map.of("message", "Instance stopped successfully"));
            }

            if (response.getFailedCount() > 0 && response.getSuccessCount() == 0) {
                return ResponseEntity.badRequest().body(response);
            } else if (response.getFailedCount() > 0) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to stop instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/restart")
    @Operation(summary = "重启实例", description = "重启单个或多个Napcat实例")
    public ResponseEntity<?> restartInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            InstanceOperationResponse response = instanceService.restartInstances(request);

            // 单个实例时返回简单响应，多个实例时返回详细响应
            if (request.getIds().size() == 1 && response.getFailedCount() == 0) {
                return ResponseEntity.ok(Map.of("message", "Instance restarted successfully"));
            }

            if (response.getFailedCount() > 0 && response.getSuccessCount() == 0) {
                return ResponseEntity.badRequest().body(response);
            } else if (response.getFailedCount() > 0) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to restart instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

    @DeleteMapping("")
    @Operation(summary = "删除实例", description = "删除单个或多个Napcat实例")
    public ResponseEntity<?> deleteInstances(@Valid @RequestBody InstanceOperationRequest request) {
        try {
            InstanceOperationResponse response = instanceService.deleteInstances(request);

            // 单个实例时返回简单响应，多个实例时返回详细响应
            if (request.getIds().size() == 1 && response.getFailedCount() == 0) {
                return ResponseEntity.ok(Map.of("message", "Instance deleted successfully"));
            }

            if (response.getFailedCount() > 0 && response.getSuccessCount() == 0) {
                return ResponseEntity.badRequest().body(response);
            } else if (response.getFailedCount() > 0) {
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to delete instances: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }

}