package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.bo.HandleCommentBO;
import com.github.zikifaker.tiktok4j.bo.HandleCommentResultBO;

public interface CommentService {
    Long getCommentCount(Long videoId);

    // 发布评论/删除评论
    HandleCommentResultBO handleComment(HandleCommentBO handleCommentBO);
}
