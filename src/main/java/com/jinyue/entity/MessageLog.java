package com.jinyue.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.jinyue.utils.PostgreSQLJSONBTypeHandler;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Accessors(chain = true)
@TableName(value = "message_log", autoResultMap = true)
public class MessageLog {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("instance_id")
    private String instanceId;

    @TableField("direction")
    private MessageDirection direction;

    @TableField("message_type")
    private String messageType;

    @TableField(value = "content", typeHandler = PostgreSQLJSONBTypeHandler.class)
    private Map<String, Object> content;

    @TableField("forwarded")
    private Boolean forwarded;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public enum MessageDirection {
        INBOUND,
        OUTBOUND
    }
}