package com.github.zikifaker.tiktok4j.config;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OSSConfig {
    private String region;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String roleARN;
    private String bucketName;

    @Bean
    public OSSClient ossClient() {
        CredentialsProvider provider = new StaticCredentialsProvider(accessKeyId, accessKeySecret);
        return OSSClient.newBuilder()
                .credentialsProvider(provider)
                .region(region)
                .endpoint(endpoint)
                .build();
    }
}
