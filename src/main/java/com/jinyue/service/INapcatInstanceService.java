package com.jinyue.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinyue.dto.InstanceOperationRequest;
import com.jinyue.dto.InstanceOperationResponse;
import com.jinyue.dto.CreateMultiInstanceRequest;
import com.jinyue.dto.InstanceResponse;
import com.jinyue.dto.MultiInstanceResponse;
import com.jinyue.entity.NapcatInstance;

import java.util.List;

public interface INapcatInstanceService extends IService<NapcatInstance> {


    /**
     * 启动实例
     * @param instanceId 实例ID
     */
    void startInstance(String instanceId);

    /**
     * 停止实例
     * @param instanceId 实例ID
     */
    void stopInstance(String instanceId);

    /**
     * 重启实例
     * @param instanceId 实例ID
     */
    void restartInstance(String instanceId);

    /**
     * 删除实例
     * @param instanceId 实例ID
     */
    void deleteInstance(String instanceId);

    /**
     * 获取实例详情
     * @param instanceId 实例ID
     * @return 实例响应
     */
    InstanceResponse getInstance(String instanceId);

    /**
     * 分页获取实例列表
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    IPage<InstanceResponse> getInstances(int page, int size);

    /**
     * 获取所有实例
     * @return 实例列表
     */
    List<InstanceResponse> getAllInstances();

    /**
     * 批量创建Napcat实例
     * @param request 批量创建请求
     * @return 批量创建响应
     */
    MultiInstanceResponse createMultipleInstances(CreateMultiInstanceRequest request);

    /**
     * 启动实例（支持单个或批量）
     * @param request 操作请求
     * @return 操作响应
     */
    InstanceOperationResponse startInstances(InstanceOperationRequest request);

    /**
     * 停止实例（支持单个或批量）
     * @param request 操作请求
     * @return 操作响应
     */
    InstanceOperationResponse stopInstances(InstanceOperationRequest request);

    /**
     * 重启实例（支持单个或批量）
     * @param request 操作请求
     * @return 操作响应
     */
    InstanceOperationResponse restartInstances(InstanceOperationRequest request);

    /**
     * 删除实例（支持单个或批量）
     * @param request 操作请求
     * @return 操作响应
     */
    InstanceOperationResponse deleteInstances(InstanceOperationRequest request);

    /**
     * 获取实例的二维码文件
     * @param instanceId 实例ID
     * @return 二维码文件字节数组，如果文件不存在返回null
     */
    byte[] getInstanceQrCode(String instanceId);
}