package com.jinyue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "实例操作响应")
public class InstanceOperationResponse {

    @Schema(description = "成功操作的实例ID列表")
    private List<String> successIds;

    @Schema(description = "失败操作的实例信息列表")
    private List<FailedOperation> failedOperations;

    @Schema(description = "总计请求数量")
    private Integer totalRequested;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Schema(description = "操作类型")
    private String operation;

    @Data
    @Schema(description = "失败的操作信息")
    public static class FailedOperation {
        @Schema(description = "实例ID")
        private String instanceId;

        @Schema(description = "失败原因")
        private String reason;
    }
}