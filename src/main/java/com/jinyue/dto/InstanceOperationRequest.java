package com.jinyue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "实例操作请求")
public class InstanceOperationRequest {

    @Schema(description = "实例ID列表", example = "[\"uuid1\", \"uuid2\", \"uuid3\"]", required = true)
    @NotEmpty(message = "实例ID列表不能为空")
    @Size(min = 1, max = 20, message = "操作实例数量必须在1-20之间")
    private List<String> ids;
}