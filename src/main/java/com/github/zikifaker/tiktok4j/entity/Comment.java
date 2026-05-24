package com.github.zikifaker.tiktok4j.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long userId;
    private Long videoId;
    private String content;
    private LocalDateTime createTime;
    private Integer cancel;
}
