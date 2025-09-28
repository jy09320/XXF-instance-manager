package com.jinyue.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jinyue.utils.NapcatConfigTypeHandler;
import com.jinyue.dto.NapcatConfig;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName(value = "napcat_instance", autoResultMap = true)
public class NapcatInstance {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("name")
    private String name;

    @TableField("container_id")
    private String containerId;

    @TableField("status")
    private InstanceStatus status;

    @TableField(value = "config", typeHandler = NapcatConfigTypeHandler.class)
    private NapcatConfig config;

    @TableField("port")
    private Integer port;

    @TableField("qq_account")
    private String qqAccount;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public enum InstanceStatus {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        ERROR,
        UNKNOWN
    }
}