package com.jinyue.service;

import com.github.dockerjava.api.model.Container;

import com.jinyue.dto.NapcatConfig;

import java.util.List;

public interface IDockerService {

    /**
     * 创建Docker容器
     * @param instanceName 实例名称
     * @param config Napcat配置信息
     * @param port 端口号 (已废弃，使用config中的端口配置)
     * @return 容器ID
     */
    String createContainer(String instanceName, NapcatConfig config, int port);

    /**
     * 启动容器
     * @param containerId 容器ID
     */
    void startContainer(String containerId);

    /**
     * 停止容器
     * @param containerId 容器ID
     */
    void stopContainer(String containerId);

    /**
     * 删除容器
     * @param containerId 容器ID
     */
    void removeContainer(String containerId);

    /**
     * 获取容器状态
     * @param containerId 容器ID
     * @return 容器状态
     */
    ContainerStatus getContainerStatus(String containerId);

    /**
     * 检查容器是否存在
     * @param containerId 容器ID
     * @return 是否存在
     */
    boolean containerExists(String containerId);

    /**
     * 列出所有Napcat容器
     * @return 容器列表
     */
    List<Container> listNapcatContainers();

    /**
     * 获取实例数据目录路径
     * @param instanceName 实例名称
     * @return 数据目录路径
     */
    String getInstanceDataPath(String instanceName);

    /**
     * 从容器中复制文件
     * @param containerId 容器ID
     * @param containerPath 容器内文件路径
     * @return 文件内容的字节数组，如果文件不存在返回null
     */
    byte[] copyFileFromContainer(String containerId, String containerPath);

    /**
     * 复制文件到容器中
     * @param containerId 容器ID
     * @param fileContent 文件内容
     * @param containerPath 容器内目标路径
     */
    void copyFileToContainer(String containerId, String fileContent, String containerPath);

    enum ContainerStatus {
        RUNNING,
        STOPPED,
        RESTARTING,
        PAUSED,
        DEAD,
        NOT_FOUND,
        UNKNOWN
    }
}