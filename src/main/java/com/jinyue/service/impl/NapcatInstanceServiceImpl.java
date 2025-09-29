package com.jinyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinyue.dto.InstanceOperationRequest;
import com.jinyue.dto.InstanceOperationResponse;
import com.jinyue.dto.CreateInstanceRequest;
import com.jinyue.dto.CreateMultiInstanceRequest;
import com.jinyue.dto.InstanceResponse;
import com.jinyue.dto.MultiInstanceResponse;
import com.jinyue.dto.NapcatConfig;
import com.jinyue.entity.NapcatInstance;
import com.jinyue.mapper.NapcatInstanceMapper;
import com.jinyue.service.IDockerService;
import com.jinyue.service.INapcatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NapcatInstanceServiceImpl extends ServiceImpl<NapcatInstanceMapper, NapcatInstance>
        implements INapcatInstanceService {

    private final IDockerService dockerService;

    @Value("${napcat.docker.base-port}")
    private int basePort;

    @Value("${napcat.instance.max-instances}")
    private int maxInstances;

    private InstanceResponse createInstance(CreateInstanceRequest request) {
        validateCreateRequest(request);

        checkInstanceLimit();

        if (existsByName(request.getName())) {
            throw new RuntimeException("Instance name already exists: " + request.getName());
        }

        // 处理配置，如果为空则使用默认配置
        NapcatConfig config = request.getConfig();
        if (config == null) {
            config = new NapcatConfig();
        }

        // 端口逻辑：优先使用config中的端口，然后是request的port，最后是自动分配
        int port = config.getServicePort();
        if (port == 6099 && request.getPort() != null) {
            // 如果config使用的是默认端口，但request指定了端口，则使用request的端口
            port = request.getPort();
            config.setServicePort(port);
        }
        if (port == 6099) {
            // 如果仍然是默认端口，则自动分配
            port = findAvailablePort();
            config.setServicePort(port);
        }

        // 确保HTTP和WebSocket端口也是唯一的，避免与其他实例冲突
        if (config.getHttpPort() == 3000) {
            config.setHttpPort(port + 1000);
        }
        if (config.getWsPort() == 3001) {
            config.setWsPort(port + 2000);
        }

        NapcatInstance instance = new NapcatInstance()
                .setName(request.getName())
                .setQqAccount(request.getQqAccount())
                .setConfig(config)
                .setPort(port)
                .setStatus(NapcatInstance.InstanceStatus.STOPPED)
                .setCreatedTime(LocalDateTime.now())
                .setUpdatedTime(LocalDateTime.now());

        if (!save(instance)) {
            throw new RuntimeException("Failed to save instance to database");
        }

        try {
            String containerId = dockerService.createContainer(
                    instance.getName(),
                    instance.getConfig(),
                    instance.getPort()
            );

            instance.setContainerId(containerId);
            updateById(instance);

            log.info("Created instance: {} with container: {}", instance.getName(), containerId);
            return InstanceResponse.from(instance);

        } catch (Exception e) {
            removeById(instance.getId());
            throw new RuntimeException("Failed to create Docker container: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void startInstance(String instanceId) {
        NapcatInstance instance = getInstanceById(instanceId);

        if (instance.getStatus() == NapcatInstance.InstanceStatus.RUNNING) {
            log.warn("Instance {} is already running", instance.getName());
            return;
        }

        try {
            instance.setStatus(NapcatInstance.InstanceStatus.STARTING);
            updateById(instance);

            dockerService.startContainer(instance.getContainerId());

            instance.setStatus(NapcatInstance.InstanceStatus.RUNNING);
            instance.setUpdatedTime(LocalDateTime.now());
            updateById(instance);

            log.info("Started instance: {}", instance.getName());

        } catch (Exception e) {
            instance.setStatus(NapcatInstance.InstanceStatus.ERROR);
            updateById(instance);
            throw new RuntimeException("Failed to start instance: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void stopInstance(String instanceId) {
        NapcatInstance instance = getInstanceById(instanceId);

        if (instance.getStatus() == NapcatInstance.InstanceStatus.STOPPED) {
            log.warn("Instance {} is already stopped", instance.getName());
            return;
        }

        try {
            instance.setStatus(NapcatInstance.InstanceStatus.STOPPING);
            updateById(instance);

            dockerService.stopContainer(instance.getContainerId());

            instance.setStatus(NapcatInstance.InstanceStatus.STOPPED);
            instance.setUpdatedTime(LocalDateTime.now());
            updateById(instance);

            log.info("Stopped instance: {}", instance.getName());

        } catch (Exception e) {
            instance.setStatus(NapcatInstance.InstanceStatus.ERROR);
            updateById(instance);
            throw new RuntimeException("Failed to stop instance: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void restartInstance(String instanceId) {
        stopInstance(instanceId);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startInstance(instanceId);
    }

    @Override
    @Transactional
    public void deleteInstance(String instanceId) {
        NapcatInstance instance = getInstanceById(instanceId);

        if (instance.getStatus() == NapcatInstance.InstanceStatus.RUNNING) {
            stopInstance(instanceId);
        }

        try {
            if (instance.getContainerId() != null &&
                dockerService.containerExists(instance.getContainerId())) {
                dockerService.removeContainer(instance.getContainerId());
            }

            removeById(instanceId);
            log.info("Deleted instance: {}", instance.getName());

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete instance: " + e.getMessage());
        }
    }

    @Override
    public InstanceResponse getInstance(String instanceId) {
        NapcatInstance instance = getInstanceById(instanceId);
        updateInstanceStatus(instance);
        return InstanceResponse.from(instance);
    }

    @Override
    public IPage<InstanceResponse> getInstances(int page, int size) {
        Page<NapcatInstance> pageRequest = new Page<>(page, size);
        IPage<NapcatInstance> instances = page(pageRequest);

        instances.getRecords().forEach(this::updateInstanceStatus);

        return instances.convert(InstanceResponse::from);
    }

    @Override
    public List<InstanceResponse> getAllInstances() {
        List<NapcatInstance> instances = list();
        instances.forEach(this::updateInstanceStatus);
        return instances.stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    private NapcatInstance getInstanceById(String instanceId) {
        NapcatInstance instance = getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("Instance not found: " + instanceId);
        }
        return instance;
    }

    private boolean existsByName(String name) {
        return count(new LambdaQueryWrapper<NapcatInstance>()
                .eq(NapcatInstance::getName, name)) > 0;
    }

    private void validateCreateRequest(CreateInstanceRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Instance name is required");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("Instance name too long");
        }
        if (!request.getName().matches("^[a-zA-Z0-9][a-zA-Z0-9_-]*$")) {
            throw new IllegalArgumentException("Instance name contains invalid characters");
        }
    }

    private void checkInstanceLimit() {
        long currentCount = count();
        if (currentCount >= maxInstances) {
            throw new RuntimeException("Maximum instance limit reached: " + maxInstances);
        }
    }

    private int findAvailablePort() {
        // 收集所有已使用的端口（服务端口、HTTP端口、WebSocket端口）
        List<Integer> usedPorts = new ArrayList<>();

        list().forEach(instance -> {
            if (instance.getPort() != null) {
                usedPorts.add(instance.getPort());
            }
            if (instance.getConfig() != null) {
                if (instance.getConfig().getHttpPort() != null) {
                    usedPorts.add(instance.getConfig().getHttpPort());
                }
                if (instance.getConfig().getWsPort() != null) {
                    usedPorts.add(instance.getConfig().getWsPort());
                }
            }
        });

        for (int port = basePort; port < basePort + 1000; port++) {
            // 检查服务端口、HTTP端口(+1000)、WebSocket端口(+2000)都不冲突
            if (!usedPorts.contains(port) &&
                !usedPorts.contains(port + 1000) &&
                !usedPorts.contains(port + 2000)) {
                return port;
            }
        }
        throw new RuntimeException("No available ports");
    }

    private void updateInstanceStatus(NapcatInstance instance) {
        if (instance.getContainerId() != null) {
            try {
                IDockerService.ContainerStatus containerStatus =
                        dockerService.getContainerStatus(instance.getContainerId());

                NapcatInstance.InstanceStatus newStatus = mapContainerStatusToInstanceStatus(containerStatus);

                if (newStatus != instance.getStatus()) {
                    instance.setStatus(newStatus);
                    instance.setUpdatedTime(LocalDateTime.now());
                    updateById(instance);
                }
            } catch (Exception e) {
                log.warn("Failed to update status for instance {}: {}",
                        instance.getName(), e.getMessage());
            }
        }
    }

    private NapcatInstance.InstanceStatus mapContainerStatusToInstanceStatus(
            IDockerService.ContainerStatus containerStatus) {
        return switch (containerStatus) {
            case RUNNING -> NapcatInstance.InstanceStatus.RUNNING;
            case STOPPED -> NapcatInstance.InstanceStatus.STOPPED;
            case RESTARTING -> NapcatInstance.InstanceStatus.STARTING;
            case NOT_FOUND, DEAD -> NapcatInstance.InstanceStatus.ERROR;
            default -> NapcatInstance.InstanceStatus.UNKNOWN;
        };
    }

    @Override
    @Transactional
    public MultiInstanceResponse createMultipleInstances(CreateMultiInstanceRequest request) {
        validateMultiCreateRequest(request);

        MultiInstanceResponse response = new MultiInstanceResponse();
        response.setTotalRequested(request.getCount());
        response.setSuccessInstances(new ArrayList<>());
        response.setFailedInstances(new ArrayList<>());

        List<Integer> availablePorts = findAvailablePorts(request.getCount());
        if (availablePorts.size() < request.getCount()) {
            throw new RuntimeException("Not enough available ports for " + request.getCount() + " instances");
        }

        for (int i = 0; i < request.getCount(); i++) {
            try {
                String instanceName = generateInstanceName(request.getNamePrefix(), i);
                String qqAccount = getQqAccountForIndex(request.getQqAccounts(), i);

                NapcatConfig config = createConfigFromTemplate(request.getConfigTemplate());
                int basePort = availablePorts.get(i);
                config.setServicePort(basePort);
                // 为每个实例分配不同的HTTP和WebSocket端口，避免冲突
                config.setHttpPort(basePort + 1000);  // 如6099 -> 7099
                config.setWsPort(basePort + 2000);    // 如6099 -> 8099

                CreateInstanceRequest singleRequest = new CreateInstanceRequest();
                singleRequest.setName(instanceName);
                singleRequest.setQqAccount(qqAccount);
                singleRequest.setConfig(config);

                InstanceResponse instanceResponse = createInstance(singleRequest);
                response.getSuccessInstances().add(instanceResponse);

                if (request.getAutoStart()) {
                    try {
                        startInstance(instanceResponse.getId());
                    } catch (Exception e) {
                        log.warn("Failed to auto-start instance {}: {}", instanceName, e.getMessage());
                    }
                }

            } catch (Exception e) {
                MultiInstanceResponse.FailedInstance failed = new MultiInstanceResponse.FailedInstance();
                failed.setName(generateInstanceName(request.getNamePrefix(), i));
                failed.setQqAccount(getQqAccountForIndex(request.getQqAccounts(), i));
                failed.setReason(e.getMessage());
                response.getFailedInstances().add(failed);

                log.error("Failed to create instance {}: {}", failed.getName(), e.getMessage());
            }
        }

        response.setSuccessCount(response.getSuccessInstances().size());
        response.setFailedCount(response.getFailedInstances().size());

        log.info("Batch creation completed: {} success, {} failed",
                response.getSuccessCount(), response.getFailedCount());

        return response;
    }

    private void validateMultiCreateRequest(CreateMultiInstanceRequest request) {
        if (request.getCount() == null || request.getCount() <= 0) {
            throw new IllegalArgumentException("Instance count must be greater than 0");
        }

        if (request.getQqAccounts() != null &&
            request.getQqAccounts().size() != request.getCount()) {
            throw new IllegalArgumentException("QQ accounts count must match instance count or be empty");
        }

        long currentCount = count();
        if (currentCount + request.getCount() > maxInstances) {
            throw new RuntimeException("Creating " + request.getCount() +
                " instances would exceed maximum limit: " + maxInstances);
        }
    }

    private List<Integer> findAvailablePorts(int count) {
        // 收集所有已使用的端口（服务端口、HTTP端口、WebSocket端口）
        List<Integer> usedPorts = new ArrayList<>();

        list().forEach(instance -> {
            if (instance.getPort() != null) {
                usedPorts.add(instance.getPort());
            }
            if (instance.getConfig() != null) {
                if (instance.getConfig().getHttpPort() != null) {
                    usedPorts.add(instance.getConfig().getHttpPort());
                }
                if (instance.getConfig().getWsPort() != null) {
                    usedPorts.add(instance.getConfig().getWsPort());
                }
            }
        });

        List<Integer> availablePorts = new ArrayList<>();
        for (int port = basePort; port < basePort + 1000 && availablePorts.size() < count; port++) {
            // 检查服务端口、HTTP端口(+1000)、WebSocket端口(+2000)都不冲突
            if (!usedPorts.contains(port) &&
                !usedPorts.contains(port + 1000) &&
                !usedPorts.contains(port + 2000)) {
                availablePorts.add(port);
            }
        }

        return availablePorts;
    }

    private String generateInstanceName(String prefix, int index) {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return String.format("%s-%s-%d", prefix, timestamp, index + 1);
    }

    private String getQqAccountForIndex(List<String> qqAccounts, int index) {
        if (qqAccounts != null && index < qqAccounts.size()) {
            return qqAccounts.get(index);
        }
        return null;
    }

    private NapcatConfig createConfigFromTemplate(NapcatConfig template) {
        if (template == null) {
            return new NapcatConfig();
        }

        NapcatConfig config = new NapcatConfig();
        config.setNapcatUid(template.getNapcatUid());
        config.setNapcatGid(template.getNapcatGid());
        config.setHttpPort(template.getHttpPort());
        config.setWsPort(template.getWsPort());
        config.setEnableHttp(template.getEnableHttp());
        config.setEnableWs(template.getEnableWs());
        config.setAutoLogin(template.getAutoLogin());
        config.setProtocol(template.getProtocol());
        config.setLogLevel(template.getLogLevel());
        config.setDebug(template.getDebug());
        config.setNetworkMode(template.getNetworkMode());
        config.setRestartPolicy(template.getRestartPolicy());
        config.setMemoryLimit(template.getMemoryLimit());
        config.setCpuLimit(template.getCpuLimit());

        return config;
    }

    @Override
    public InstanceOperationResponse startInstances(InstanceOperationRequest request) {
        return performInstanceOperation(request.getIds(), "START", this::startInstance);
    }

    @Override
    public InstanceOperationResponse stopInstances(InstanceOperationRequest request) {
        return performInstanceOperation(request.getIds(), "STOP", this::stopInstance);
    }

    @Override
    public InstanceOperationResponse restartInstances(InstanceOperationRequest request) {
        return performInstanceOperation(request.getIds(), "RESTART", this::restartInstance);
    }

    @Override
    public InstanceOperationResponse deleteInstances(InstanceOperationRequest request) {
        return performInstanceOperation(request.getIds(), "DELETE", this::deleteInstance);
    }

    private InstanceOperationResponse performInstanceOperation(
            List<String> instanceIds,
            String operation,
            InstanceOperationHandler operationHandler) {

        InstanceOperationResponse response = new InstanceOperationResponse();
        response.setOperation(operation);
        response.setTotalRequested(instanceIds.size());
        response.setSuccessIds(new ArrayList<>());
        response.setFailedOperations(new ArrayList<>());

        for (String instanceId : instanceIds) {
            try {
                operationHandler.perform(instanceId);
                response.getSuccessIds().add(instanceId);
                log.info("Successfully performed {} operation on instance: {}", operation, instanceId);
            } catch (Exception e) {
                InstanceOperationResponse.FailedOperation failed = new InstanceOperationResponse.FailedOperation();
                failed.setInstanceId(instanceId);
                failed.setReason(e.getMessage());
                response.getFailedOperations().add(failed);
                log.error("Failed to perform {} operation on instance {}: {}", operation, instanceId, e.getMessage());
            }
        }

        response.setSuccessCount(response.getSuccessIds().size());
        response.setFailedCount(response.getFailedOperations().size());

        log.info("{} operation completed: {} success, {} failed",
                operation, response.getSuccessCount(), response.getFailedCount());

        return response;
    }

    @Override
    public byte[] getInstanceQrCode(String instanceId) {
        try {
            // 获取实例信息
            NapcatInstance instance = getInstanceById(instanceId);

            // 检查实例是否有容器ID
            if (instance.getContainerId() == null) {
                log.warn("Instance {} has no container ID", instance.getName());
                return null;
            }

            // 使用Docker API从容器中复制二维码文件
            String qrCodePath = "/app/napcat/cache/qrcode.png";
            byte[] qrCodeBytes = dockerService.copyFileFromContainer(instance.getContainerId(), qrCodePath);

            if (qrCodeBytes == null) {
                log.warn("QR code file not found for instance {}: {}", instance.getName(), qrCodePath);
                return null;
            }

            log.info("Successfully retrieved QR code for instance {}, file size: {} bytes",
                    instance.getName(), qrCodeBytes.length);

            return qrCodeBytes;

        } catch (RuntimeException e) {
            log.error("Failed to get QR code for instance {}: {}", instanceId, e.getMessage());
            throw e;
        }
    }

    @FunctionalInterface
    private interface InstanceOperationHandler {
        void perform(String instanceId) throws Exception;
    }
}