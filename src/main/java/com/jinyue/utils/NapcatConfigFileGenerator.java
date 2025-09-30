package com.jinyue.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NapcatConfigFileGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成 OneBot 11 配置文件内容
     * @param qqAccount QQ账号
     * @param webhookUrl Webhook上报地址
     * @return JSON格式的配置文件内容
     */
    public String generateOneBotConfig(String qqAccount, String webhookUrl) {
        try {
            Map<String, Object> config = new HashMap<>();

            // 网络配置
            Map<String, Object> network = new HashMap<>();
            network.put("httpServers", new ArrayList<>());
            network.put("httpClients", createHttpClients(qqAccount, webhookUrl));
            network.put("websocketServers", new ArrayList<>());
            network.put("websocketClients", new ArrayList<>());

            config.put("network", network);
            config.put("musicSignUrl", "");
            config.put("enableLocalFile2Url", false);
            config.put("parseMultMsg", false);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);

        } catch (Exception e) {
            log.error("Failed to generate OneBot config for QQ account {}: {}", qqAccount, e.getMessage());
            throw new RuntimeException("Failed to generate OneBot config", e);
        }
    }

    /**
     * 创建HTTP客户端配置
     */
    private List<Map<String, Object>> createHttpClients(String qqAccount, String webhookUrl) {
        List<Map<String, Object>> httpClients = new ArrayList<>();

        Map<String, Object> client = new HashMap<>();
        client.put("name", qqAccount);
        client.put("enable", true);
        client.put("url", webhookUrl);
        client.put("messagePostFormat", "array");
        client.put("reportSelfMessage", false);
        client.put("token", "");
        client.put("debug", false);

        httpClients.add(client);
        return httpClients;
    }

    /**
     * 获取配置文件名
     * @param qqAccount QQ账号
     * @return 配置文件名
     */
    public String getConfigFileName(String qqAccount) {
        return "onebot11_" + qqAccount + ".json";
    }

    /**
     * 获取配置文件在容器中的完整路径
     * @param qqAccount QQ账号
     * @return 容器中的配置文件路径
     */
    public String getConfigFilePath(String qqAccount) {
        return "/app/napcat/config/" + getConfigFileName(qqAccount);
    }
}