package com.github.zikifaker.tiktok4j.service;

public interface FollowService {
    Long getFolloweeCount(Long followerId);

    Long getFollowerCount(Long followeeId);

    Boolean isFollowed(Long currentUserId, Long targetUserId);
}
