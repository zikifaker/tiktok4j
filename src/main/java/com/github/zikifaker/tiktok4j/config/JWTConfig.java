package com.github.zikifaker.tiktok4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JWTConfig {
    private String secretKey;
    private Integer expireDays;
}
