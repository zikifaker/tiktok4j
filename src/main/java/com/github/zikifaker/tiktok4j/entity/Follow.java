package com.github.zikifaker.tiktok4j.entity;

import lombok.Data;

@Data
public class Follow {
    private Long id;

    // 关注用户 id
    private Long followerId;

    // 被关注用户 id
    private Long followeeId;

    private Integer cancel;
}
