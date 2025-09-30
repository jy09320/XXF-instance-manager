package com.jinyue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    private String status;
    private String message;

    public static WebhookResponse success(String message) {
        return new WebhookResponse("success", message);
    }

    public static WebhookResponse error(String message) {
        return new WebhookResponse("error", message);
    }
}