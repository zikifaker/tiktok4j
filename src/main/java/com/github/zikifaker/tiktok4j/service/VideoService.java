package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.bo.PublishVideoBO;
import com.github.zikifaker.tiktok4j.bo.VideoBO;

import java.time.LocalDateTime;
import java.util.List;

public interface VideoService {
    List<VideoBO> getFeed(Long userId, LocalDateTime lastTime);

    void publishVideo(PublishVideoBO publishVideoBO);

    List<VideoBO> getUserVideos(Long userId);

    List<VideoBO> getUserLikeVideos(Long currentUserId, Long targetUserId);
}
