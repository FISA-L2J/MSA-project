package com.msa.account_service.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfig {

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    @Bean
    public Connection natsConnection() throws Exception {
        io.nats.client.Options options = io.nats.client.Options.builder()
                .server(natsUrl)
                .connectionTimeout(java.time.Duration.ofSeconds(10)) // 타임아웃 10초로 증가
                .build();
        return Nats.connect(options);
    }

}
