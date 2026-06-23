package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.enums.FollowActionType;
import com.github.zikifaker.tiktok4j.consts.MQConstants;
import com.github.zikifaker.tiktok4j.consts.RedisKeys;
import com.github.zikifaker.tiktok4j.mapper.FollowMapper;
import com.github.zikifaker.tiktok4j.mq.message.ToggleFollowMessage;
import com.github.zikifaker.tiktok4j.service.FollowService;
import com.github.zikifaker.tiktok4j.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class FollowServiceImpl implements FollowService {
    private StringRedisTemplate cacheService;

    private RocketMQTemplate mqService;

    private UserService userService;

    private FollowMapper followMapper;

    private static final int CACHE_EXPIRE_DAYS = 30;

    @Autowired
    public FollowServiceImpl(
            StringRedisTemplate cacheService,
            RocketMQTemplate mqService,
            UserService userService,
            FollowMapper followMapper
    ) {
        this.cacheService = cacheService;
        this.mqService = mqService;
        this.userService = userService;
        this.followMapper = followMapper;
    }

    @Override
    public void toggleFollow(Long currentUserId, Long targetUserId, FollowActionType actionType) {
        switch (actionType) {
            case FOLLOW -> doFollow(currentUserId, targetUserId);
            case UNFOLLOW -> doUnfollow(currentUserId, targetUserId);
        }
    }

    private void doFollow(Long followerId, Long followeeId) {
        // 维护用户关注缓存
        String userFolloweesKey = String.format(RedisKeys.USER_FOLLOWEES, followerId);
        if (Boolean.TRUE.equals(cacheService.hasKey(userFolloweesKey))) {
            cacheService.opsForSet().add(userFolloweesKey, String.valueOf(followeeId));
        } else {
            List<Long> followeeIds = followMapper.getFolloweeIds(followerId);
            String[] ids = Stream
                    .concat(followeeIds.stream().map(String::valueOf), Stream.of(String.valueOf(followeeId)))
                    .toArray(String[]::new);
            cacheService.expire(userFolloweesKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userFolloweesKey, ids);
        }

        // 维护用户粉丝缓存
        String userFollowersKey = String.format(RedisKeys.USER_FOLLOWERS, followeeId);
        if (Boolean.TRUE.equals(cacheService.hasKey(userFollowersKey))) {
            cacheService.opsForSet().add(userFollowersKey, String.valueOf(followerId));
        } else {
            List<Long> followerIds = followMapper.getFollowerIds(followeeId);
            String[] ids = Stream
                    .concat(followerIds.stream().map(String::valueOf), Stream.of(String.valueOf(followerId)))
                    .toArray(String[]::new);
            cacheService.expire(userFollowersKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userFollowersKey, ids);
        }

        // 向 MQ 发送消息
        sendToggleFollowMessage(followerId, followeeId, FollowActionType.FOLLOW);
    }

    private void doUnfollow(Long followerId, Long followeeId) {
        String userFolloweesKey = String.format(RedisKeys.USER_FOLLOWEES, followerId);
        if (Boolean.TRUE.equals(cacheService.hasKey(userFolloweesKey))) {
            cacheService.opsForSet().remove(userFolloweesKey, String.valueOf(followeeId));
        } else {
            List<Long> followeeIds = followMapper.getFolloweeIds(followerId);
            String[] ids = followeeIds.stream()
                    .filter(id -> !id.equals(followeeId))
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(userFolloweesKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userFolloweesKey, ids);
        }

        String userFollowersKey = String.format(RedisKeys.USER_FOLLOWERS, followeeId);
        if (Boolean.TRUE.equals(cacheService.hasKey(userFollowersKey))) {
            cacheService.opsForSet().remove(userFollowersKey, String.valueOf(followerId));
        } else {
            List<Long> followerIds = followMapper.getFollowerIds(followeeId);
            String[] ids = followerIds.stream()
                    .filter(id -> !id.equals(followerId))
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(userFollowersKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userFollowersKey, ids);
        }

        sendToggleFollowMessage(followerId, followeeId, FollowActionType.UNFOLLOW);
    }

    private void sendToggleFollowMessage(
            Long followerId,
            Long followeeId,
            FollowActionType actionType
    ) {
        ToggleFollowMessage message = new ToggleFollowMessage(
                followerId,
                followeeId,
                actionType.name()
        );
        String tag = (actionType == FollowActionType.FOLLOW) ? MQConstants.TAG_FOLLOW : MQConstants.TAG_UNFOLLOW;
        String destination = String.format("%s:%s", MQConstants.TOPIC_TIKTOK_FOLLOW, tag);
        mqService.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("Sent toggle follow message: followerId={}, followeeId={}, action={}",
                        followerId,
                        followeeId,
                        actionType
                );
            }

            @Override
            public void onException(Throwable e) {
                log.error("Failed to send toggle follow message: followerId={}, followeeId={}, action={}, error={}",
                        followerId,
                        followeeId,
                        actionType,
                        e.getMessage()
                );
            }
        });
    }

    @Override
    public List<UserInfoBO> getFollowers(Long currentUserId, Long targetUserId) {
        String userFollowersKey = String.format(RedisKeys.USER_FOLLOWERS, targetUserId);
        Boolean exists = cacheService.hasKey(userFollowersKey);
        Set<String> result;

        if (Boolean.TRUE.equals(exists)) {
            result = cacheService.opsForSet().members(userFollowersKey);
        } else {
            List<Long> followerIds = followMapper.getFollowerIds(targetUserId);
            String[] ids = followerIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(userFollowersKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userFollowersKey, ids);
            result = Set.of(ids);
        }

        return result.stream()
                .map(Long::valueOf)
                .map(followerId -> userService.getUserInfo(currentUserId, followerId))
                .collect(Collectors.toList());
    }
}
