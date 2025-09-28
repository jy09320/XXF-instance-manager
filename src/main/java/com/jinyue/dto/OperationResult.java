package com.jinyue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OperationResult {
    String instanceId;
    boolean success;
    String errorMessage;
}
