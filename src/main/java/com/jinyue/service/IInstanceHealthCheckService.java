package com.jinyue.service;

/**
 * 实例健康检查服务接口
 * 用于应用启动时检查实例状态并标记异常
 */
public interface IInstanceHealthCheckService {

    /**
     * 应用启动时执行健康检查
     * 扫描所有实例，检查容器状态，标记异常实例
     */
    void performStartupHealthCheck();

    /**
     * 修复单个实例的容器状态
     * @param instanceId 实例ID
     * @return 是否修复成功
     */
    boolean repairInstanceContainer(String instanceId);
}
