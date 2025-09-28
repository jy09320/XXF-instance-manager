package com.jinyue.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MessageDto {

    private String type;

    private Map<String, Object> data;

    private Long timestamp;

    private String instanceId;
}