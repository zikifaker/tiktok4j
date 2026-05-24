package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.mapper.FollowMapper;
import com.github.zikifaker.tiktok4j.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FollowServiceImpl implements FollowService {
    private FollowMapper followMapper;

    @Autowired
    public FollowServiceImpl(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    @Override
    public Long getFolloweeCount(Long followerId) {
        return followMapper.getFolloweeCount(followerId);
    }

    @Override
    public Long getFollowerCount(Long followedId) {
        return followMapper.getFollowerCount(followedId);
    }

    @Override
    public Boolean isFollowed(Long currentUserId, Long targetUserId) {
        return followMapper.isFollowed(currentUserId, targetUserId);
    }
}
