package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.enums.FollowActionType;

public interface FollowService {
    Long getFolloweeCount(Long followerId);

    Long getFollowerCount(Long followeeId);

    Boolean isFollowed(Long currentUserId, Long targetUserId);

    void toggleFollow(Long currentUserId, Long targetUserId, FollowActionType actionType);
}
