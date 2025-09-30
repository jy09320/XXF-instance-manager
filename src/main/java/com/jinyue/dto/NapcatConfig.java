package com.jinyue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Napcat实例配置")
public class NapcatConfig {

    @Schema(description = "Napcat进程用户ID", example = "1000", defaultValue = "1000")
    private Integer napcatUid = 1000;

    @Schema(description = "Napcat进程用户组ID", example = "1000", defaultValue = "1000")
    private Integer napcatGid = 1000;

    @Schema(description = "HTTP API端口", example = "3000", defaultValue = "3000")
    private Integer httpPort = 3000;

    @Schema(description = "WebSocket端口", example = "3001", defaultValue = "3001")
    private Integer wsPort = 3001;

    @Schema(description = "服务端口", example = "6099", defaultValue = "6099")
    private Integer servicePort = 6099;

    @Schema(description = "是否启用HTTP接口", example = "true", defaultValue = "true")
    private Boolean enableHttp = true;

    @Schema(description = "是否启用WebSocket接口", example = "true", defaultValue = "true")
    private Boolean enableWs = true;

    @Schema(description = "是否自动登录", example = "true", defaultValue = "true")
    private Boolean autoLogin = true;

    @Schema(description = "协议版本 (1-安卓手机, 2-安卓平板, 3-安卓手表, 4-MacOS, 5-iPad)",
            example = "1", defaultValue = "1", allowableValues = {"1", "2", "3", "4", "5"})
    private Integer protocol = 1;

    @Schema(description = "日志级别", example = "info", defaultValue = "info",
            allowableValues = {"trace", "debug", "info", "warn", "error", "fatal", "off"})
    private String logLevel = "info";

    @Schema(description = "是否启用调试模式", example = "false", defaultValue = "false")
    private Boolean debug = false;

    @Schema(description = "网络模式", example = "bridge", defaultValue = "bridge")
    private String networkMode = "bridge";

    @Schema(description = "重启策略", example = "always", defaultValue = "always",
            allowableValues = {"no", "always", "on-failure", "unless-stopped"})
    private String restartPolicy = "always";

    @Schema(description = "内存限制 (如: 512m, 1g)", example = "512m", defaultValue = "512m")
    private String memoryLimit = "512m";

    @Schema(description = "CPU限制 (如: 0.5, 1.0)", example = "1.0", defaultValue = "1.0")
    private Double cpuLimit = 1.0;
}