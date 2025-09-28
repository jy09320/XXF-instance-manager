package com.jinyue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建Napcat实例请求")
public class CreateInstanceRequest {

    @Schema(description = "实例名称", example = "napcat-instance-1", required = true)
    private String name;

    @Schema(description = "QQ账号", example = "123456789", required = true)
    private String qqAccount;

    @Schema(description = "Napcat配置", required = false)
    private NapcatConfig config;

    @Schema(description = "实例端口 (已废弃，请使用config中的端口配置)", example = "6099", deprecated = true)
    private Integer port;
}