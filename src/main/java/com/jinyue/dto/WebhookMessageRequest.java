package com.jinyue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * NapCat Webhook消息请求体
 * 用于接收NapCat通过Webhook推送的消息数据
 */
@Data
public class WebhookMessageRequest {

    /**
     * 上报类型，固定为"message"表示消息类型
     */
    @NotNull
    @JsonProperty("post_type")
    private String postType;

    /**
     * 消息类型
     * "group" - 群组消息
     * "private" - 私聊消息
     */
    @NotNull
    @JsonProperty("message_type")
    private String messageType;

    /**
     * 消息发送时间戳（秒级）
     */
    @NotNull
    private Long time;

    /**
     * 机器人自己的QQ号
     */
    @NotNull
    @JsonProperty("self_id")
    private Long selfId;

    /**
     * 消息ID，用于唯一标识一条消息
     */
    @NotNull
    @JsonProperty("message_id")
    private Long messageId;

    /**
     * 发送者QQ号
     */
    @NotNull
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 群号（仅群消息时存在）
     */
    @JsonProperty("group_id")
    private Long groupId;

    /**
     * 群名称（仅群消息时存在）
     */
    @JsonProperty("group_name")
    private String groupName;

    /**
     * 原始消息内容
     */
    @NotNull
    @JsonProperty("raw_message")
    private String rawMessage;

    /**
     * 发送者信息
     */
    @NotNull
    @Valid
    private Sender sender;

    /**
     * 消息发送者信息
     */
    @Data
    public static class Sender {
        /**
         * 发送者QQ号
         */
        @NotNull
        @JsonProperty("user_id")
        private Long userId;

        /**
         * 发送者昵称
         */
        @NotNull
        private String nickname;

        /**
         * 群名片（仅群消息时存在）
         */
        private String card;
    }
}