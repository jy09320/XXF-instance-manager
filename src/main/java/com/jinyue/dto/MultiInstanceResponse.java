package com.jinyue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量创建实例响应")
public class MultiInstanceResponse {

    @Schema(description = "成功创建的实例列表")
    private List<InstanceResponse> instances;

    @Schema(description = "创建失败的信息列表")
    private List<FailedInstance> errors;

    @Schema(description = "总计请求数量")
    private Integer totalRequested;

    @Schema(description = "成功创建数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Data
    @Schema(description = "创建失败的实例信息")
    public static class FailedInstance {
        @Schema(description = "实例名称")
        private String name;

        @Schema(description = "QQ账号（如果有）")
        private String qqAccount;

        @Schema(description = "失败原因")
        private String reason;
    }
}