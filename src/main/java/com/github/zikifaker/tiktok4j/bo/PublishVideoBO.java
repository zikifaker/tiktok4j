package com.github.zikifaker.tiktok4j.bo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublishVideoBO {
    private String title;
    private Long userId;
    private String objectKey;
}
