package com.jinyue.controller;

import com.jinyue.dto.CreateMultiInstanceRequest;
import com.jinyue.dto.InstanceOperationRequest;
import com.jinyue.dto.InstanceResponse;
import com.jinyue.dto.MultiInstanceResponse;
import com.jinyue.exception.InvalidInstanceStateException;
import com.jinyue.exception.InstanceNotFoundException;
import com.jinyue.service.INapcatInstanceService;
import com.jinyue.entity.NapcatInstance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * QQ号路由层Controller
 * 提供基于QQ号的实例管理接口,内部自动解析UUID并调用原有UUID接口
 * 用于后端和Proxy服务,无需关心实例UUID
 */
@Slf4j
@RestController
@RequestMapping("/api/qq-instances")
@RequiredArgsConstructor
@Tag(name = "QQ号实例管理", description = "基于QQ号的实例管理接口(内部自动路由到UUID)")
public class QQInstanceController {

    private final INapcatInstanceService instanceService;

    /**
     * 通过QQ号解析实例UUID
     */
    private String resolveUuidByQQ(String qqNumber) {
        NapcatInstance instance = instanceService.lambdaQuery()
                .eq(NapcatInstance::getQqAccount, qqNumber)
                .one();

        if (instance == null) {
            throw new InstanceNotFoundException("QQ号 " + qqNumber + " 对应的实例不存在");
        }

        return instance.getId();
    }

    /**
     * 创建实例 (通过QQ号)
     */
    @PostMapping("/{qqNumber}")
    @Operation(summary = "创建Napcat实例(通过QQ号)", description = "根据QQ号创建实例,如果已存在则返回错误")
    public ResponseEntity<?> createInstance(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            // 检查是否已存在
            NapcatInstance existing = instanceService.lambdaQuery()
                    .eq(NapcatInstance::getQqAccount, qqNumber)
                    .one();

            if (existing != null) {
                log.warn("Instance already exists for QQ: {}", qqNumber);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "实例已存在",
                        "qq_number", qqNumber,
                        "instance_id", existing.getId()
                ));
            }

            // 调用原有创建接口
            CreateMultiInstanceRequest request = new CreateMultiInstanceRequest();
            request.setCount(1);  // 设置实例数量
            request.setQqAccounts(Collections.singletonList(qqNumber));
            request.setAutoStart(false);  // 不自动启动

            MultiInstanceResponse response = instanceService.createMultipleInstances(request);

            if (response.getSuccessCount() > 0) {
                log.info("Successfully created instance for QQ: {}", qqNumber);
                return ResponseEntity.status(HttpStatus.CREATED).body(response.getInstances().get(0));
            } else {
                log.error("Failed to create instance for QQ: {}", qqNumber);
                return ResponseEntity.internalServerError().body(Map.of(
                        "error", "创建实例失败",
                        "details", response.getErrors()
                ));
            }
        } catch (Exception e) {
            log.error("Error creating instance for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "服务器内部错误",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 启动实例 (通过QQ号)
     */
    @PutMapping("/{qqNumber}/start")
    @Operation(summary = "启动实例(通过QQ号)", description = "根据QQ号启动对应的Napcat实例")
    public ResponseEntity<?> startInstance(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            String uuid = resolveUuidByQQ(qqNumber);
            log.info("Starting instance for QQ {}, UUID: {}", qqNumber, uuid);

            // 调用service启动实例
            instanceService.startInstance(uuid);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "启动请求已提交",
                    "qq_number", qqNumber,
                    "instance_id", uuid
            ));

        } catch (InstanceNotFoundException e) {
            log.warn("Instance not found for QQ: {}", qqNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error starting instance for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "启动实例失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 停止实例 (通过QQ号)
     */
    @PutMapping("/{qqNumber}/stop")
    @Operation(summary = "停止实例(通过QQ号)", description = "根据QQ号停止对应的Napcat实例")
    public ResponseEntity<?> stopInstance(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            String uuid = resolveUuidByQQ(qqNumber);
            log.info("Stopping instance for QQ {}, UUID: {}", qqNumber, uuid);

            // 调用service停止实例
            instanceService.stopInstance(uuid);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "停止请求已提交",
                    "qq_number", qqNumber,
                    "instance_id", uuid
            ));

        } catch (InstanceNotFoundException e) {
            log.warn("Instance not found for QQ: {}", qqNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error stopping instance for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "停止实例失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 重启实例 (通过QQ号)
     */
    @PutMapping("/{qqNumber}/restart")
    @Operation(summary = "重启实例(通过QQ号)", description = "根据QQ号重启对应的Napcat实例")
    public ResponseEntity<?> restartInstance(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            String uuid = resolveUuidByQQ(qqNumber);
            log.info("Restarting instance for QQ {}, UUID: {}", qqNumber, uuid);

            // 调用service重启实例
            instanceService.restartInstance(uuid);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "重启请求已提交",
                    "qq_number", qqNumber,
                    "instance_id", uuid
            ));

        } catch (InstanceNotFoundException e) {
            log.warn("Instance not found for QQ: {}", qqNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error restarting instance for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "重启实例失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除实例 (通过QQ号)
     */
    @DeleteMapping("/{qqNumber}")
    @Operation(summary = "删除实例(通过QQ号)", description = "根据QQ号删除对应的Napcat实例")
    public ResponseEntity<?> deleteInstance(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            String uuid = resolveUuidByQQ(qqNumber);
            log.info("Deleting instance for QQ {}, UUID: {}", qqNumber, uuid);

            // 调用service删除实例
            instanceService.deleteInstance(uuid);

            return ResponseEntity.accepted().body(Map.of(
                    "message", "删除请求已提交",
                    "qq_number", qqNumber,
                    "instance_id", uuid
            ));

        } catch (InstanceNotFoundException e) {
            log.warn("Instance not found for QQ: {}", qqNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting instance for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "删除实例失败",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取二维码 (通过QQ号)
     */
    @GetMapping("/{qqNumber}/qrcode")
    @Operation(summary = "获取实例二维码(通过QQ号)", description = "根据QQ号获取登录二维码")
    public ResponseEntity<byte[]> getQRCode(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            String uuid = resolveUuidByQQ(qqNumber);
            log.info("Getting QR code for QQ {}, UUID: {}", qqNumber, uuid);

            // 调用原有二维码接口
            byte[] qrCodeBytes = instanceService.getInstanceQrCode(uuid);

            if (qrCodeBytes == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(qrCodeBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(qrCodeBytes);

        } catch (InstanceNotFoundException e) {
            log.warn("Instance not found for QQ: {}", qqNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting QR code for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取实例状态 (通过QQ号)
     */
    @GetMapping("/{qqNumber}/status")
    @Operation(summary = "获取实例状态(通过QQ号)", description = "根据QQ号获取实例状态信息")
    public ResponseEntity<?> getInstanceStatus(
            @Parameter(description = "QQ号") @PathVariable String qqNumber) {
        try {
            String uuid = resolveUuidByQQ(qqNumber);
            InstanceResponse instance = instanceService.getInstance(uuid);

            return ResponseEntity.ok(Map.of(
                    "qq_number", qqNumber,
                    "instance_id", uuid,
                    "status", instance.getStatus(),
                    "port", instance.getPort(),
                    "details", instance
            ));

        } catch (InstanceNotFoundException e) {
            log.warn("Instance not found for QQ: {}", qqNumber);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting status for QQ {}: {}", qqNumber, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取实例状态失败",
                    "message", e.getMessage()
            ));
        }
    }
}
