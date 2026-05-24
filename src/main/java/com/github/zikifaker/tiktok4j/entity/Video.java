package com.github.zikifaker.tiktok4j.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Video {
    private Long id;
    private Long authorId;
    private String sourceURL;
    private String playURL;
    private String coverURL;
    private LocalDateTime publishTime;
    private String title;
}
