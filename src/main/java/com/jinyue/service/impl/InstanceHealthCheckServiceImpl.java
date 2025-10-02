package com.jinyue.service.impl;

import com.jinyue.dto.CreateInstanceRequest;
import com.jinyue.dto.NapcatConfig;
import com.jinyue.entity.NapcatInstance;
import com.jinyue.service.IDockerService;
import com.jinyue.service.IInstanceHealthCheckService;
import com.jinyue.service.INapcatInstanceService;
import com.jinyue.utils.NapcatConfigFileGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 实例健康检查服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceHealthCheckServiceImpl implements IInstanceHealthCheckService {

    private final INapcatInstanceService instanceService;
    private final IDockerService dockerService;
    private final NapcatConfigFileGenerator configFileGenerator;

    @Value("${server.port}")
    private int serverPort;

    /**
     * 应用启动完成后自动执行健康检查
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    @Override
    public void performStartupHealthCheck() {
        try {
            // 延迟10秒执行，确保所有服务（包括Docker）都已初始化
            log.info("健康检查将在10秒后执行...");
            TimeUnit.SECONDS.sleep(10);

            log.info("=== 开始执行实例健康检查 ===");
            doHealthCheck();
        } catch (InterruptedException e) {
            log.error("健康检查延迟执行被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("健康检查执行失败", e);
        }
    }

    /**
     * 执行健康检查逻辑
     */
    private void doHealthCheck() {
        List<NapcatInstance> allInstances = instanceService.list();
        if (allInstances.isEmpty()) {
            log.info("没有实例需要检查");
            return;
        }

        int totalCount = allInstances.size();
        int missingContainerCount = 0;
        int errorCount = 0;

        for (NapcatInstance instance : allInstances) {
            try {
                // 检查容器是否存在
                if (instance.getContainerId() == null) {
                    log.warn("实例 {} (QQ: {}) 没有容器ID，标记为需要修复",
                            instance.getName(), instance.getQqAccount());
                    markInstanceAsNeedRepair(instance);
                    missingContainerCount++;
                    continue;
                }

                if (!dockerService.containerExists(instance.getContainerId())) {
                    log.warn("实例 {} (QQ: {}) 的容器 {} 不存在，标记为需要修复",
                            instance.getName(), instance.getQqAccount(), instance.getContainerId());
                    markInstanceAsNeedRepair(instance);
                    missingContainerCount++;
                } else {
                    log.debug("实例 {} (QQ: {}) 容器状态正常", instance.getName(), instance.getQqAccount());
                }
            } catch (Exception e) {
                log.error("检查实例 {} 时发生错误: {}", instance.getName(), e.getMessage());
                errorCount++;
            }
        }

        log.info("=== 健康检查完成 === 总数: {}, 容器缺失: {}, 错误: {}",
                totalCount, missingContainerCount, errorCount);

        if (missingContainerCount > 0) {
            log.warn("发现 {} 个实例的容器缺失，已标记状态。启动这些实例时将自动重建容器", missingContainerCount);
        }
    }

    /**
     * 标记实例为需要修复状态
     */
    private void markInstanceAsNeedRepair(NapcatInstance instance) {
        // 如果实例状态是RUNNING，改为STOPPED（因为容器不存在了）
        if (instance.getStatus() == NapcatInstance.InstanceStatus.RUNNING) {
            instance.setStatus(NapcatInstance.InstanceStatus.STOPPED);
            instanceService.updateById(instance);
            log.info("已将实例 {} 状态从 RUNNING 修改为 STOPPED", instance.getName());
        }
    }

    /**
     * 修复实例容器（在启动时调用）
     */
    @Override
    public boolean repairInstanceContainer(String instanceId) {
        try {
            NapcatInstance instance = instanceService.getById(instanceId);
            if (instance == null) {
                log.error("实例不存在: {}", instanceId);
                return false;
            }

            // 检查容器是否存在
            if (instance.getContainerId() != null &&
                dockerService.containerExists(instance.getContainerId())) {
                log.debug("实例 {} 容器存在，无需修复", instance.getName());
                return true;
            }

            log.info("实例 {} (QQ: {}) 容器缺失，开始重建容器", instance.getName(), instance.getQqAccount());

            // 重建容器配置
            CreateInstanceRequest repairRequest = new CreateInstanceRequest();
            repairRequest.setName(instance.getName());
            repairRequest.setQqAccount(instance.getQqAccount());
            repairRequest.setConfig(instance.getConfig() != null ? instance.getConfig() : new NapcatConfig());

            // 重新创建容器（使用原有配置）
            String newContainerId = createContainerForInstance(instance);

            if (newContainerId != null) {
                // 更新实例的容器ID
                instance.setContainerId(newContainerId);
                instance.setStatus(NapcatInstance.InstanceStatus.STOPPED);
                instanceService.updateById(instance);

                log.info("实例 {} 容器重建成功，新容器ID: {}", instance.getName(), newContainerId);
                return true;
            } else {
                log.error("实例 {} 容器重建失败", instance.getName());
                return false;
            }

        } catch (Exception e) {
            log.error("修复实例 {} 容器时发生错误: {}", instanceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 为实例创建新容器
     */
    private String createContainerForInstance(NapcatInstance instance) {
        try {
            // 使用 DockerService 创建容器
            // createContainer(instanceName, config, port)
            String containerId = dockerService.createContainer(
                instance.getName(),
                instance.getConfig(),
                instance.getPort()
            );

            if (containerId != null && instance.getQqAccount() != null && !instance.getQqAccount().isEmpty()) {
                // 重要：修复容器时也需要生成并复制OneBot配置文件
                try {
                    generateAndCopyConfigFile(containerId, instance.getQqAccount());
                    log.info("容器修复时已生成并复制配置文件到容器 {}", containerId);
                } catch (Exception e) {
                    log.warn("容器修复时复制配置文件失败，但不影响容器创建: {}", e.getMessage());
                }
            }

            return containerId;
        } catch (Exception e) {
            log.error("创建容器失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成并复制OneBot配置文件到容器
     */
    private void generateAndCopyConfigFile(String containerId, String qqAccount) {
        try {
            // 构建webhook URL - 使用host.docker.internal访问宿主机服务
            String webhookUrl = "http://host.docker.internal:" + serverPort + "/api/webhook/message";

            // 生成配置文件内容
            String configContent = configFileGenerator.generateOneBotConfig(qqAccount, webhookUrl);

            // 获取配置文件在容器中的路径
            String configPath = configFileGenerator.getConfigFilePath(qqAccount);

            // 复制配置文件到容器
            dockerService.copyFileToContainer(containerId, configContent, configPath);

            log.info("Successfully generated and copied OneBot config file for QQ account: {} to container: {}",
                    qqAccount, containerId);

        } catch (Exception e) {
            log.error("Failed to generate and copy config file for QQ account: {} to container: {}: {}",
                    qqAccount, containerId, e.getMessage());
            // 抛出异常让上层处理
            throw new RuntimeException("Failed to generate and copy config file", e);
        }
    }
}
