package com.github.zikifaker.tiktok4j.bo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetPublishTokenBO {
    private String version;
    private String policy;
    private String date;
    private String credential;
    private String signature;
    private String securityToken;
    private String objectKeyPrefix;
    private String host;
}
