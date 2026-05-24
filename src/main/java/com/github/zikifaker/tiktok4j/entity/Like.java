package com.github.zikifaker.tiktok4j.entity;

import lombok.Data;

@Data
public class Like {
    private Long Id;
    private Long userId;
    private Long videoId;
    private Integer cancel;
}
