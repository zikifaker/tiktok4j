package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.enums.LikeActionType;

public interface LikeService {
    // 获取视频点赞数
    Long getVideoLikeCount(Long videoId);

    // 视频是否被用户点赞
    Boolean isLiked(Long videoId, Long userId);

    // 获取用户总获赞数
    Long getUserTotalLikeCount(Long userId);

    // 用户点赞/取消点赞视频
    void toggleLike(Long userId, Long videoId, LikeActionType type);
}
