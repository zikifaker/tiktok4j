package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.consts.RedisKeys;
import com.github.zikifaker.tiktok4j.mapper.CommentMapper;
import com.github.zikifaker.tiktok4j.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CommentServiceImpl implements CommentService {
    private StringRedisTemplate cacheService;

    private CommentMapper commentMapper;

    private static final int CACHE_EXPIRE_DAYS = 30;

    @Autowired
    public CommentServiceImpl(StringRedisTemplate cacheService, CommentMapper commentMapper) {
        this.cacheService = cacheService;
        this.commentMapper = commentMapper;
    }

    @Override
    public Long getCommentCount(Long videoId) {
        String key = String.format(RedisKeys.VIDEO_COMMENTS, videoId);
        Boolean exists = cacheService.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            Long count = cacheService.opsForSet().size(key);
            return count != null ? count : 0L;
        }

        List<Long> commentIds = commentMapper.getCommentIds(videoId);
        if (!commentIds.isEmpty()) {
            String[] ids = commentIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(key, ids);
        }

        Long count = cacheService.opsForSet().size(key);
        return count != null ? count : 0L;
    }
}
