package com.jinyue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


import java.util.List;

@Data
@Schema(description = "批量创建Napcat实例请求")
public class CreateMultiInstanceRequest {

    @Schema(description = "实例数量", example = "3", required = true)
    @Min(value = 1, message = "实例数量不能小于1")
    @Max(value = 10, message = "实例数量不能超过10")
    private Integer count;

    @Schema(description = "QQ账号列表（可选，如果提供则数量必须与count一致）", example = "[\"123456789\", \"987654321\", \"111222333\"]")
    private List<String> qqAccounts;

    @Schema(description = "实例名称前缀", example = "napcat-batch", defaultValue = "napcat-auto")
    private String namePrefix = "napcat-auto";

    @Schema(description = "是否自动启动实例", example = "false", defaultValue = "false")
    private Boolean autoStart = false;

    @Schema(description = "共享配置模板（可选，为空则使用默认配置）")
    private NapcatConfig configTemplate;
}