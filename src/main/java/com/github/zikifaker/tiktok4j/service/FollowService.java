package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.enums.FollowActionType;

import java.util.List;

public interface FollowService {
    void toggleFollow(Long currentUserId, Long targetUserId, FollowActionType actionType);

    List<UserInfoBO> getFollowees(Long currentUserId, Long targetUserId);

    List<UserInfoBO> getFollowers(Long currentUserId, Long targetUserId);
}
