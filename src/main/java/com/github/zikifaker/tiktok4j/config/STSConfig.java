package com.github.zikifaker.tiktok4j.config;

import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import com.aliyun.sts20150401.Client;
import org.springframework.context.annotation.Configuration;

@Configuration
public class STSConfig {
    private OSSConfig ossConfig;

    private final String STS_ENDPOINT = "sts.cn-shanghai.aliyuncs.com";

    @Autowired
    public STSConfig(OSSConfig ossConfig){
        this.ossConfig = ossConfig;
    }

    @Bean
    public Client stsClient() {
        try {
            Config config = new Config()
                    .setAccessKeyId(ossConfig.getAccessKeyId())
                    .setAccessKeySecret(ossConfig.getAccessKeySecret())
                    .setEndpoint(STS_ENDPOINT);
            return new Client(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
