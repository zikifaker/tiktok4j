package com.github.zikifaker.tiktok4j.bo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoBO {
    private Long userId;
    private String username;
    private Long followeeCount;
    private Long followerCount;
    private Boolean isFollowed;
    private Long workCount;
    private Long likeCount;
}
