package com.jinyue.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class DockerConfig {

    @Value("${napcat.docker.host:tcp://localhost:2375}")
    private String dockerHost;

    @Value("${napcat.docker.tls-verify:false}")
    private boolean tlsVerify;

    @Value("${napcat.docker.cert-path:}")
    private String certPath;

    @Bean
    public DockerClient dockerClient() {
        try {
            DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .withDockerTlsVerify(tlsVerify);

            if (tlsVerify && !certPath.isEmpty()) {
                configBuilder.withDockerCertPath(certPath);
            }

            DockerClientConfig config = configBuilder.build();

            DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .connectTimeout(30000)
                    .readTimeout(45000)
                    .build();

            DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

            dockerClient.pingCmd().exec();
            log.info("Docker client connected successfully to: {}", dockerHost);

            return dockerClient;
        } catch (Exception e) {
            log.error("Failed to connect to Docker: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize Docker client", e);
        }
    }
}