package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.bo.PublishVideoBO;
import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.consts.RedisKeys;
import com.github.zikifaker.tiktok4j.entity.User;
import com.github.zikifaker.tiktok4j.entity.Video;
import com.github.zikifaker.tiktok4j.mapper.LikeMapper;
import com.github.zikifaker.tiktok4j.mapper.UserMapper;
import com.github.zikifaker.tiktok4j.mapper.VideoMapper;
import com.github.zikifaker.tiktok4j.service.CommentService;
import com.github.zikifaker.tiktok4j.service.LikeService;
import com.github.zikifaker.tiktok4j.service.VideoService;
import com.github.zikifaker.tiktok4j.utils.VideoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoServiceImpl implements VideoService {
    private static final int VIDEO_FEED_LIMIT = 30;

    private static final int CACHE_EXPIRE_DAYS = 30;

    private VideoMapper videoMapper;

    private UserMapper userMapper;

    private LikeMapper likeMapper;

    private LikeService likeService;

    private CommentService commentService;

    private StringRedisTemplate cacheService;

    private Executor threadPool;

    private VideoUtils videoUtils;

    public VideoServiceImpl(
            VideoMapper videoMapper,
            UserMapper userMapper,
            LikeMapper likeMapper,
            LikeService likeService,
            CommentService commentService,
            StringRedisTemplate cacheService,
            @Qualifier("videoTaskExecutor") Executor threadPool,
            VideoUtils videoUtils
    ) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
        this.likeMapper = likeMapper;
        this.likeService = likeService;
        this.commentService = commentService;
        this.cacheService = cacheService;
        this.threadPool = threadPool;
        this.videoUtils = videoUtils;
    }

    @Override
    public List<VideoBO> getFeed(Long userId, LocalDateTime lastTime) {
        List<Video> videos = videoMapper.getVideosByLastTime(lastTime, VIDEO_FEED_LIMIT);
        return videos.stream()
                .map(video -> buildVideoBO(video, userId))
                .toList();
    }

    @Override
    public void publishVideo(PublishVideoBO publishVideoBO) {
        try {
            // 视频路径：videos/raw/{userId}/{timestamp}/{fileName}.ext
            String rawVideoObjectKey = publishVideoBO.getObjectKey();

            // 封面路径：covers/{userId}/{timestamp}/{fileName}.jpg
            String coverObjectKey = rawVideoObjectKey
                    .replaceFirst("^([^/]+/){2}", "covers/")
                    .replaceAll("\\.[^.]+$", ".jpg");

            // 视频转码产物路径前缀：videos/hls/{userId}/{timestamp}/{fileName}
            String hlsObjectKeyPrefix = rawVideoObjectKey
                    .replaceFirst("^([^/]+/){2}", "videos/hls/")
                    .replaceAll("\\.[^.]+$", "");

            // 提取并上传视频封面
            Path rawVideo = videoUtils.extractAndUploadFirstFrame(rawVideoObjectKey, coverObjectKey);
            log.info("Extracted and uploaded first frame successfully: {}", rawVideoObjectKey);

            // 转码切分视频并上传
            videoUtils.convertAndUploadVideo(rawVideo, hlsObjectKeyPrefix);
            log.info("Converted and uploaded video successfully: {}", rawVideoObjectKey);

            // 存储元数据至 MySQL
            Video video = Video.builder()
                    .title(publishVideoBO.getTitle())
                    .authorId(publishVideoBO.getUserId())
                    .sourceURL(rawVideoObjectKey)
                    .playURL(hlsObjectKeyPrefix + "/index.m3u8")
                    .coverURL(coverObjectKey)
                    .publishTime(Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime())
                    .build();
            videoMapper.save(video);
        } catch (Exception e) {
            log.error("Error publishing video: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private VideoBO buildVideoBO(Video video, Long userId) {
        CompletableFuture<User> authorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return userMapper.getUserById(userId);
            } catch (Exception e) {
                log.error("Error getting user by user id: {}", e.getMessage());
                return new User();
            }
        }, threadPool);

        CompletableFuture<Long> likeCountFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return likeService.getVideoLikeCount(video.getId());
            } catch (Exception e) {
                log.error("Error getting like count for video {}: {}", video.getId(), e.getMessage());
                return 0L;
            }
        }, threadPool);

        CompletableFuture<Long> commentCountFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return commentService.getCommentCount(video.getId());
            } catch (Exception e) {
                log.error("Error getting comment count for video {}: {}", video.getId(), e.getMessage());
                return 0L;
            }
        }, threadPool);

        CompletableFuture<Boolean> isLikedFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return likeService.isLiked(video.getId(), userId);
            } catch (Exception e) {
                log.error("Error checking if user {} liked video {}: {}", userId, video.getId(), e.getMessage());
                return false;
            }
        }, threadPool);

        CompletableFuture.allOf(
                authorFuture,
                likeCountFuture,
                commentCountFuture,
                isLikedFuture
        );

        return VideoBO.builder()
                .video(video)
                .author(authorFuture.join())
                .likeCount(likeCountFuture.join())
                .commentCount(commentCountFuture.join())
                .isLike(isLikedFuture.join())
                .build();
    }

    @Override
    public List<VideoBO> getUserVideos(Long userId) {
        List<Video> videos = videoMapper.getUserVideos(userId);
        return videos.stream()
                .map(video -> buildVideoBO(video, userId))
                .toList();
    }

    @Override
    public List<VideoBO> getUserLikeVideos(Long currentUserId, Long targetUserId) {
        String userLikeVideosKey = String.format(RedisKeys.USER_LIKE_VIDEOS, targetUserId);
        Boolean exists = cacheService.hasKey(userLikeVideosKey);
        Set<String> videoIdStrs;

        // 查询用户点赞视频 id 缓存，若未命中访问数据库
        if (Boolean.TRUE.equals(exists)) {
            videoIdStrs = cacheService.opsForSet().members(userLikeVideosKey);
        } else {
            List<Long> videoIds = likeMapper.getLikeVideoIds(targetUserId);
            String[] ids = videoIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            cacheService.expire(userLikeVideosKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            cacheService.opsForSet().add(userLikeVideosKey, ids);
            videoIdStrs = cacheService.opsForSet().members(userLikeVideosKey);
        }

        if (videoIdStrs == null || videoIdStrs.isEmpty()) {
            return List.of();
        }

        List<Long> ids = videoIdStrs.stream()
                .map(Long::valueOf)
                .toList();
        List<Video> videos = videoMapper.getVideosByIds(ids);
        return videos.stream()
                .map(video -> buildVideoBO(video, currentUserId))
                .toList();
    }
}
