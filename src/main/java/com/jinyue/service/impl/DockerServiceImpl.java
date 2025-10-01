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

import java.util.ArrayList;
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
        } catch (com.github.dockerjava.api.exception.NotModifiedException e) {
            // 304 状态码：容器已经停止，不需要再次停止
            log.info("Container {} is already stopped, skipping", containerId);
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
        } catch (com.github.dockerjava.api.exception.NotFoundException e) {
            // 404 状态码：容器不存在，可能已经被删除
            log.info("Container {} not found, may already be removed", containerId);
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

        // 使用命名Volume替代Bind挂载
        String volumeName = "napcat-data-" + instanceName;
        String containerDataPath = "/app/napcat";

        // 创建Volume挂载绑定
        binds.add(new Bind(volumeName, new Volume(containerDataPath)));

        log.debug("Configured volume mount: {} -> {}", volumeName, containerDataPath);
        return binds;
    }

    /**
     * 获取实例数据目录路径
     */
    public String getInstanceDataPath(String instanceName) {
        // 对于Volume挂载，返回Volume名称
        return "napcat-data-" + instanceName;
    }

    /**
     * 从容器中复制文件
     */
    @Override
    public byte[] copyFileFromContainer(String containerId, String containerPath) {
        try {
            // 检查容器是否存在且运行中
            if (!containerExists(containerId)) {
                log.warn("Container {} not found", containerId);
                return null;
            }

            ContainerStatus status = getContainerStatus(containerId);
            if (status != ContainerStatus.RUNNING) {
                log.warn("Container {} is not running, status: {}", containerId, status);
                return null;
            }

            // 使用Docker API复制文件
            try (var inputStream = dockerClient.copyArchiveFromContainerCmd(containerId, containerPath).exec()) {
                // 读取tar格式的输入流
                return extractFileFromTarStream(inputStream);
            }

        } catch (Exception e) {
            log.error("Failed to copy file {} from container {}: {}", containerPath, containerId, e.getMessage());
            return null;
        }
    }

    /**
     * 从tar流中提取文件内容
     */
    private byte[] extractFileFromTarStream(java.io.InputStream tarStream) throws java.io.IOException {
        try (var bufferedStream = new java.io.BufferedInputStream(tarStream);
             var byteOutput = new java.io.ByteArrayOutputStream()) {

            // 简单的tar文件头解析
            byte[] header = new byte[512];

            while (bufferedStream.read(header) == 512) {
                // 获取文件大小（tar头部的124-135字节，8进制表示）
                String sizeStr = new String(header, 124, 11).trim();
                if (sizeStr.isEmpty()) continue;

                long size;
                try {
                    size = Long.parseLong(sizeStr, 8);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (size > 0) {
                    // 读取文件内容
                    byte[] fileContent = new byte[(int) size];
                    int bytesRead = bufferedStream.read(fileContent);
                    if (bytesRead > 0) {
                        return java.util.Arrays.copyOf(fileContent, bytesRead);
                    }
                }

                // 跳过padding到512字节边界
                long padding = (512 - (size % 512)) % 512;
                bufferedStream.skip(padding);
            }
        }
        return null;
    }

    /**
     * 复制文件到容器中
     */
    @Override
    public void copyFileToContainer(String containerId, String fileContent, String containerPath) {
        try {
            // 检查容器是否存在
            if (!containerExists(containerId)) {
                throw new RuntimeException("Container " + containerId + " not found");
            }

            // 创建临时tar包含文件内容
            byte[] tarData = createTarArchive(fileContent, getFileNameFromPath(containerPath));

            // 获取目标目录路径（去掉文件名）
            String targetDir = getDirectoryFromPath(containerPath);

            // 复制文件到容器
            try (var inputStream = new java.io.ByteArrayInputStream(tarData)) {
                dockerClient.copyArchiveToContainerCmd(containerId)
                        .withTarInputStream(inputStream)
                        .withRemotePath(targetDir)
                        .exec();
            }

            log.info("Successfully copied file to container {}: {}", containerId, containerPath);

        } catch (Exception e) {
            log.error("Failed to copy file to container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to copy file to container", e);
        }
    }

    /**
     * 创建包含单个文件的tar归档
     */
    private byte[] createTarArchive(String fileContent, String fileName) throws java.io.IOException {
        try (var byteOutput = new java.io.ByteArrayOutputStream();
             var tarOutput = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(byteOutput)) {

            byte[] contentBytes = fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // 创建tar条目
            org.apache.commons.compress.archivers.tar.TarArchiveEntry entry =
                new org.apache.commons.compress.archivers.tar.TarArchiveEntry(fileName);
            entry.setSize(contentBytes.length);
            entry.setMode(0644); // 设置文件权限

            tarOutput.putArchiveEntry(entry);
            tarOutput.write(contentBytes);
            tarOutput.closeArchiveEntry();
            tarOutput.finish();

            return byteOutput.toByteArray();
        }
    }

    /**
     * 从路径中提取文件名
     */
    private String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * 从路径中提取目录路径
     */
    private String getDirectoryFromPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : "/";
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