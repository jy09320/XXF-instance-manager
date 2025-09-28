package com.jinyue.config;

import com.jinyue.websocket.ClientWebSocketHandler;
import com.jinyue.websocket.NapcatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NapcatWebSocketHandler napcatWebSocketHandler;
    private final ClientWebSocketHandler clientWebSocketHandler;

    @Value("${napcat.websocket.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Napcat实例连接端点
        registry.addHandler(napcatWebSocketHandler, "/ws/napcat/*")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        // 客户端连接端点
        registry.addHandler(clientWebSocketHandler, "/ws/client")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}