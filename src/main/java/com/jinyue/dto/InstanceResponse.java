package com.jinyue.dto;

import com.jinyue.entity.NapcatInstance;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Napcat实例响应")
public class InstanceResponse {

    @Schema(description = "实例ID")
    private String id;

    @Schema(description = "实例名称")
    private String name;

    @Schema(description = "Docker容器ID")
    private String containerId;

    @Schema(description = "实例状态")
    private NapcatInstance.InstanceStatus status;

    @Schema(description = "Napcat配置")
    private NapcatConfig config;

    @Schema(description = "端口号")
    private Integer port;

    @Schema(description = "QQ账号")
    private String qqAccount;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    public static InstanceResponse from(NapcatInstance instance) {
        InstanceResponse response = new InstanceResponse();
        response.setId(instance.getId());
        response.setName(instance.getName());
        response.setContainerId(instance.getContainerId());
        response.setStatus(instance.getStatus());
        response.setConfig(instance.getConfig());
        response.setPort(instance.getPort());
        response.setQqAccount(instance.getQqAccount());
        response.setCreatedTime(instance.getCreatedTime());
        response.setUpdatedTime(instance.getUpdatedTime());
        return response;
    }
}