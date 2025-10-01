package com.jinyue.dto;

import lombok.Builder;
import lombok.Data;

/**
 * NapCat实例访问信息
 * 用于proxy根据UUID路由消息到具体的NapCat实例
 */
@Data
@Builder
public class InstanceAccessInfo {

    /**
     * 实例UUID
     */
    private String uuid;

    /**
     * 绑定的QQ账号
     */
    private String qqAccount;

    /**
     * NapCat实例的HTTP API地址
     * 格式: http://localhost:{port}
     */
    private String napcatUrl;

    /**
     * NapCat实例的Token (可选)
     */
    private String napcatToken;

    /**
     * 实例当前状态
     * STOPPED, STARTING, RUNNING, STOPPING, ERROR, UNKNOWN
     */
    private String status;

    /**
     * 实例端口号
     */
    private Integer port;
}