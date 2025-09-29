package com.jinyue.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.jinyue.dto.NapcatConfig;
import com.jinyue.service.IDockerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DockerServiceImpl implements IDockerService {

    private final DockerClient dockerClient;

    @Value("${napcat.docker.image}")
    private String napcatImage;

    @Value("${napcat.docker.network}")
    private String networkName;

    @Value("${napcat.docker.container-prefix}")
    private String containerPrefix;

    @Value("${napcat.docker.data-dir}")
    private String dataDir;

    @Override
    public String createContainer(String instanceName, NapcatConfig config, int port) {
        try {
            String containerName = containerPrefix + "-" + instanceName;

            pullImageIfNotExists();

            // 使用配置或默认值
            if (config == null) {
                config = new NapcatConfig();
            }

            // 构建端口绑定
            List<PortBinding> portBindings = buildPortBindings(config);

            // 构建暴露端口
            List<ExposedPort> exposedPorts = buildExposedPorts(config);

            // 构建挂载点
            List<Bind> binds = buildBinds(instanceName);

            CreateContainerResponse container = dockerClient.createContainerCmd(napcatImage)
                    .withName(containerName)
                    .withHostConfig(HostConfig.newHostConfig()
                            .withPortBindings(portBindings)
                            .withNetworkMode(config.getNetworkMode())
                            .withMemory(parseMemoryLimit(config.getMemoryLimit()))
                            .withCpuQuota((long)(config.getCpuLimit() * 100000))
                            .withAutoRemove(false)
                            .withRestartPolicy(getRestartPolicy(config.getRestartPolicy()))
                            .withBinds(binds))
                    .withEnv(buildEnvironmentVariables(config))
                    .withExposedPorts(exposedPorts)
                    .withLabels(java.util.Map.of(
                            "napcat.instance", instanceName,
                            "napcat.manager", "xxf-instance-manager"
                    ))
                    .exec();

            log.info("Created container {} for instance {}", container.getId(), instanceName);
            return container.getId();

        } catch (Exception e) {
            log.error("Failed to create container for instance {}: {}", instanceName, e.getMessage());
            throw new RuntimeException("Failed to create Docker container", e);
        }
    }

    @Override
    public void startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Started container: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to start container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to start container", e);
        }
    }

    @Override
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(30)
                    .exec();
            log.info("Stopped container: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to stop container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to stop container", e);
        }
    }

    @Override
    public void removeContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
            log.info("Removed container: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to remove container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to remove container", e);
        }
    }

    @Override
    public ContainerStatus getContainerStatus(String containerId) {
        try {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            InspectContainerResponse.ContainerState state = container.getState();

            if (Boolean.TRUE.equals(state.getRunning())) {
                return ContainerStatus.RUNNING;
            } else if (Boolean.TRUE.equals(state.getRestarting())) {
                return ContainerStatus.RESTARTING;
            } else if (Boolean.TRUE.equals(state.getPaused())) {
                return ContainerStatus.PAUSED;
            } else if (Boolean.TRUE.equals(state.getDead())) {
                return ContainerStatus.DEAD;
            } else {
                return ContainerStatus.STOPPED;
            }
        } catch (NotFoundException e) {
            return ContainerStatus.NOT_FOUND;
        } catch (Exception e) {
            log.error("Failed to get container status for {}: {}", containerId, e.getMessage());
            return ContainerStatus.UNKNOWN;
        }
    }

    @Override
    public boolean containerExists(String containerId) {
        try {
            dockerClient.inspectContainerCmd(containerId).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception e) {
            log.warn("Error checking container existence for {}: {}", containerId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Container> listNapcatContainers() {
        try {
            return dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withLabelFilter(Map.of("napcat.manager", "xxf-instance-manager"))
                    .exec();
        } catch (Exception e) {
            log.error("Failed to list napcat containers: {}", e.getMessage());
            throw new RuntimeException("Failed to list containers", e);
        }
    }

    private void pullImageIfNotExists() throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(napcatImage).exec();
            log.debug("Image {} already exists", napcatImage);
        } catch (NotFoundException e) {
            log.info("Pulling image: {}", napcatImage);
            dockerClient.pullImageCmd(napcatImage)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            log.info("Successfully pulled image: {}", napcatImage);
        }
    }

    private List<String> buildEnvironmentVariables(NapcatConfig config) {
        List<String> env = new ArrayList<>();
        env.add("NAPCAT_UID=" + config.getNapcatUid());
        env.add("NAPCAT_GID=" + config.getNapcatGid());

        // 添加其他环境变量
        if (config.getDebug()) {
            env.add("NAPCAT_DEBUG=true");
        }
        env.add("NAPCAT_LOG_LEVEL=" + config.getLogLevel());
        env.add("NAPCAT_AUTO_LOGIN=" + config.getAutoLogin());
        env.add("NAPCAT_PROTOCOL=" + config.getProtocol());
        env.add("NAPCAT_HTTP_PORT=" + config.getHttpPort());
        env.add("NAPCAT_WS_PORT=" + config.getWsPort());
        env.add("NAPCAT_ENABLE_HTTP=" + config.getEnableHttp());
        env.add("NAPCAT_ENABLE_WS=" + config.getEnableWs());

        return env;
    }

    private List<PortBinding> buildPortBindings(NapcatConfig config) {
        List<PortBinding> bindings = new ArrayList<>();

        // HTTP端口
        if (config.getEnableHttp()) {
            bindings.add(new PortBinding(
                    Ports.Binding.bindPort(config.getHttpPort()),
                    new ExposedPort(config.getHttpPort(), InternetProtocol.TCP)
            ));
        }

        // WebSocket端口
        if (config.getEnableWs()) {
            bindings.add(new PortBinding(
                    Ports.Binding.bindPort(config.getWsPort()),
                    new ExposedPort(config.getWsPort(), InternetProtocol.TCP)
            ));
        }

        // 服务端口
        bindings.add(new PortBinding(
                Ports.Binding.bindPort(config.getServicePort()),
                new ExposedPort(config.getServicePort(), InternetProtocol.TCP)
        ));

        return bindings;
    }

    private List<ExposedPort> buildExposedPorts(NapcatConfig config) {
        List<ExposedPort> ports = new ArrayList<>();

        if (config.getEnableHttp()) {
            ports.add(new ExposedPort(config.getHttpPort(), InternetProtocol.TCP));
        }

        if (config.getEnableWs()) {
            ports.add(new ExposedPort(config.getWsPort(), InternetProtocol.TCP));
        }

        ports.add(new ExposedPort(config.getServicePort(), InternetProtocol.TCP));

        return ports;
    }

    private long parseMemoryLimit(String memoryLimit) {
        if (memoryLimit == null || memoryLimit.isEmpty()) {
            return 512 * 1024 * 1024L; // 默认512MB
        }

        memoryLimit = memoryLimit.toLowerCase().trim();
        if (memoryLimit.endsWith("g")) {
            return Long.parseLong(memoryLimit.replace("g", "")) * 1024 * 1024 * 1024L;
        } else if (memoryLimit.endsWith("m")) {
            return Long.parseLong(memoryLimit.replace("m", "")) * 1024 * 1024L;
        } else {
            return Long.parseLong(memoryLimit);
        }
    }

    private RestartPolicy getRestartPolicy(String restartPolicy) {
        return switch (restartPolicy) {
            case "always" -> RestartPolicy.alwaysRestart();
            case "on-failure" -> RestartPolicy.onFailureRestart(3);
            case "unless-stopped" -> RestartPolicy.unlessStoppedRestart();
            default -> RestartPolicy.noRestart();
        };
    }

    private List<Bind> buildBinds(String instanceName) {
        List<Bind> binds = new ArrayList<>();

        // 构建实例数据目录路径
        String hostDataPath = dataDir + "/instances/" + instanceName;
        String containerDataPath = "/app/napcat";

        // 确保宿主机目录存在
        File hostDir = new File(hostDataPath);
        if (!hostDir.exists()) {
            boolean created = hostDir.mkdirs();
            if (created) {
                log.info("Created data directory for instance {}: {}", instanceName, hostDataPath);
            } else {
                log.warn("Failed to create data directory for instance {}: {}", instanceName, hostDataPath);
            }
        }

        // 创建挂载绑定
        binds.add(new Bind(hostDataPath, new Volume(containerDataPath)));

        log.debug("Configured mount: {} -> {}", hostDataPath, containerDataPath);
        return binds;
    }

    /**
     * 获取实例数据目录路径
     */
    public String getInstanceDataPath(String instanceName) {
        return dataDir + "/instances/" + instanceName;
    }

    private static class PullImageResultCallback extends com.github.dockerjava.api.async.ResultCallback.Adapter<PullResponseItem> {
        @Override
        public void onNext(PullResponseItem item) {
            if (item.getStatus() != null) {
                log.debug("Pull status: {}", item.getStatus());
            }
        }
    }
}