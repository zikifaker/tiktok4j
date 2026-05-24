package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.bo.PublishVideoBO;
import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.entity.User;
import com.github.zikifaker.tiktok4j.entity.Video;
import com.github.zikifaker.tiktok4j.mapper.UserMapper;
import com.github.zikifaker.tiktok4j.mapper.VideoMapper;
import com.github.zikifaker.tiktok4j.service.CommentService;
import com.github.zikifaker.tiktok4j.service.LikeService;
import com.github.zikifaker.tiktok4j.service.VideoService;
import com.github.zikifaker.tiktok4j.utils.VideoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class VideoServiceImpl implements VideoService {
    private static final int VIDEO_FEED_LIMIT = 30;

    private VideoMapper videoMapper;

    private UserMapper userMapper;

    private LikeService likeService;

    private CommentService commentService;

    private Executor threadPool;

    private VideoUtils videoUtils;

    public VideoServiceImpl(
            VideoMapper videoMapper,
            UserMapper userMapper,
            LikeService likeService,
            CommentService commentService,
            @Qualifier("videoTaskExecutor") Executor threadPool,
            VideoUtils videoUtils
    ) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
        this.likeService = likeService;
        this.commentService = commentService;
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
    public Long getWorkCount(Long userId) {
        return videoMapper.getVideoCount(userId);
    }

    @Override
    public List<VideoBO> getUserVideos(Long userId) {
        List<Video> videos = videoMapper.getUserVideos(userId);
        return videos.stream()
                .map(video -> buildVideoBO(video, userId))
                .toList();
    }
}
