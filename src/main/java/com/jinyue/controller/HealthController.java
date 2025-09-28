package com.jinyue.controller;

import com.github.dockerjava.api.DockerClient;
import com.jinyue.service.INapcatInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "健康检查", description = "系统健康状态检查")
public class HealthController {

    private final DockerClient dockerClient;
    private final INapcatInstanceService instanceService;

    @GetMapping
    @Operation(summary = "系统健康检查", description = "检查系统各组件的健康状态")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();

        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // 检查Docker连接
        try {
            dockerClient.pingCmd().exec();
            checks.put("docker", Map.of("status", "UP", "message", "Docker is accessible"));
        } catch (Exception e) {
            checks.put("docker", Map.of("status", "DOWN", "message", "Docker connection failed: " + e.getMessage()));
            health.put("status", "DOWN");
        }

        // 检查数据库连接
        try {
            long instanceCount = instanceService.count();
            checks.put("database", Map.of("status", "UP", "message", "Database is accessible", "instanceCount", instanceCount));
        } catch (Exception e) {
            checks.put("database", Map.of("status", "DOWN", "message", "Database connection failed: " + e.getMessage()));
            health.put("status", "DOWN");
        }

        health.put("checks", checks);

        if ("DOWN".equals(health.get("status"))) {
            return ResponseEntity.status(503).body(health);
        }

        return ResponseEntity.ok(health);
    }

    @GetMapping("/simple")
    @Operation(summary = "简单健康检查", description = "简单的健康状态检查")
    public ResponseEntity<Map<String, String>> simpleHealthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "Napcat Instance Manager"));
    }
}