package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.enums.LikeActionType;
import com.github.zikifaker.tiktok4j.consts.MQConstants;
import com.github.zikifaker.tiktok4j.consts.RedisKeys;
import com.github.zikifaker.tiktok4j.mapper.LikeMapper;
import com.github.zikifaker.tiktok4j.mq.message.ToggleLikeMessage;
import com.github.zikifaker.tiktok4j.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Service
public class LikeServiceImpl implements LikeService {
    private StringRedisTemplate cacheService;

    private RocketMQTemplate mqService;

    private LikeMapper likeMapper;

    private static final int CACHE_EXPIRE_DAYS = 30;

    @Autowired
    public LikeServiceImpl(
            StringRedisTemplate cacheService,
            RocketMQTemplate mqService,
            LikeMapper likeMapper
    ) {
        this.cacheService = cacheService;
        this.mqService = mqService;
        this.likeMapper = likeMapper;
    }

    @Override
    public Long getVideoLikeCount(Long videoId) {
        String key = String.format(RedisKeys.LIKE_VIDEO_USERS, videoId);
        Boolean exists = cacheService.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            Long count = cacheService.opsForSet().size(key);
            return count != null ? count : 0L;
        }

        List<Long> userIds = likeMapper.getLikeUserIds(videoId);
        if (!userIds.isEmpty()) {
            String[] ids = userIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(key, ids);
        }

        Long count = cacheService.opsForSet().size(key);
        return count != null ? count : 0L;
    }

    @Override
    public Boolean isLiked(Long videoId, Long userId) {
        // 查询用户点赞的视频 id 缓存
        String userLikeVideosKey = String.format(RedisKeys.USER_LIKE_VIDEOS, userId);
        Boolean exists = cacheService.hasKey(userLikeVideosKey);
        if (Boolean.TRUE.equals(exists)) {
            Boolean isMember = cacheService.opsForSet().isMember(userLikeVideosKey, String.valueOf(videoId));
            return Boolean.TRUE.equals(isMember);
        }

        // 查询点赞视频的用户 id 缓存
        String likeVideoUsersKey = String.format(RedisKeys.LIKE_VIDEO_USERS, videoId);
        exists = cacheService.hasKey(likeVideoUsersKey);
        if (Boolean.TRUE.equals(exists)) {
            Boolean isMember = cacheService.opsForSet().isMember(likeVideoUsersKey, String.valueOf(userId));
            return Boolean.TRUE.equals(isMember);
        }

        // 从数据库查询用户点赞的视频 id，并加载到缓存
        List<Long> videoIds = likeMapper.getLikeVideoIds(userId);
        if (!videoIds.isEmpty()) {
            String[] ids = videoIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(userLikeVideosKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userLikeVideosKey, ids);
        }

        Boolean isMember = cacheService.opsForSet().isMember(userLikeVideosKey, String.valueOf(videoId));
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public Long getUserTotalLikeCount(Long userId) {
        return likeMapper.getUserTotalLikeCount(userId);
    }

    @Override
    public void toggleLike(Long userId, Long videoId, LikeActionType type) {
        switch (type) {
            case LIKE -> doLike(userId, videoId);
            case UNLIKE -> doUnlike(userId, videoId);
        }
    }

    private void doLike(Long userId, Long videoId) {
        // 维护用户点赞的视频 id
        String userLikeVideosKey = String.format(RedisKeys.USER_LIKE_VIDEOS, userId);
        if (Boolean.TRUE.equals(cacheService.hasKey(userLikeVideosKey))) {
            cacheService.opsForSet().add(userLikeVideosKey, String.valueOf(videoId));
        } else {
            List<Long> videoIds = likeMapper.getLikeVideoIds(userId);
            if (!videoIds.isEmpty()) {
                String[] ids = Stream
                        .concat(videoIds.stream().map(String::valueOf), Stream.of(String.valueOf(videoId)))
                        .toArray(String[]::new);
                cacheService.expire(userLikeVideosKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                cacheService.opsForSet().add(userLikeVideosKey, ids);
            }
        }

        // 维护点赞视频的用户 id
        String likeVideoUsersKey = String.format(RedisKeys.LIKE_VIDEO_USERS, videoId);
        if (Boolean.TRUE.equals(cacheService.hasKey(likeVideoUsersKey))) {
            cacheService.opsForSet().add(likeVideoUsersKey, String.valueOf(userId));
        } else {
            List<Long> userIds = likeMapper.getLikeUserIds(videoId);
            if (!userIds.isEmpty()) {
                String[] ids = Stream
                        .concat(userIds.stream().map(String::valueOf), Stream.of(String.valueOf(userId)))
                        .toArray(String[]::new);
                cacheService.expire(likeVideoUsersKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                cacheService.opsForSet().add(likeVideoUsersKey, ids);
            }
        }

        // 向 MQ 发送消息
        sendToggleLikeMessage(userId, videoId, LikeActionType.LIKE);
    }

    private void doUnlike(Long userId, Long videoId) {
        String userLikeVideosKey = String.format(RedisKeys.USER_LIKE_VIDEOS, userId);
        if (Boolean.TRUE.equals(cacheService.hasKey(userLikeVideosKey))) {
            cacheService.opsForSet().remove(userLikeVideosKey, String.valueOf(videoId));
        } else {
            List<Long> videoIds = likeMapper.getLikeVideoIds(userId);
            if (!videoIds.isEmpty()) {
                String[] ids = videoIds.stream()
                        .filter(id -> !id.equals(videoId))
                        .map(String::valueOf)
                        .toArray(String[]::new);
                cacheService.expire(userLikeVideosKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                cacheService.opsForSet().add(userLikeVideosKey, ids);
            }
        }

        String likeVideoUsersKey = String.format(RedisKeys.LIKE_VIDEO_USERS, videoId);
        if (Boolean.TRUE.equals(cacheService.hasKey(likeVideoUsersKey))) {
            cacheService.opsForSet().remove(likeVideoUsersKey, String.valueOf(userId));
        } else {
            List<Long> userIds = likeMapper.getLikeUserIds(videoId);
            if (!userIds.isEmpty()) {
                String[] ids = userIds.stream()
                        .filter(id -> !id.equals(userId))
                        .map(String::valueOf)
                        .toArray(String[]::new);
                cacheService.expire(likeVideoUsersKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                cacheService.opsForSet().add(likeVideoUsersKey, ids);
            }
        }

        sendToggleLikeMessage(userId, videoId, LikeActionType.UNLIKE);
    }

    private void sendToggleLikeMessage(
            Long userId,
            Long videoId,
            LikeActionType actionType
    ) {
        ToggleLikeMessage message = new ToggleLikeMessage(
                userId,
                videoId,
                actionType.name()
        );
        String destination = String.format("%s:%s", MQConstants.TOPIC_TIKTOK_LIKE, MQConstants.TAG_TOGGLE);
        mqService.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("Sent toggle like message: userId={}, videoId={}, action={}",
                        userId,
                        videoId,
                        actionType
                );
            }

            @Override
            public void onException(Throwable e) {
                log.error("Failed to send toggle like message: userId={}, videoId={}, action={}, error={}",
                        userId,
                        videoId,
                        actionType,
                        e.getMessage()
                );
            }
        });
    }
}
