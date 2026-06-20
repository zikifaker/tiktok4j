package com.github.zikifaker.tiktok4j.service.impl;

import com.github.zikifaker.tiktok4j.bo.GetCommentListBO;
import com.github.zikifaker.tiktok4j.bo.HandleCommentBO;
import com.github.zikifaker.tiktok4j.bo.HandleCommentResultBO;
import com.github.zikifaker.tiktok4j.consts.MQConstants;
import com.github.zikifaker.tiktok4j.consts.RedisKeys;
import com.github.zikifaker.tiktok4j.entity.Comment;
import com.github.zikifaker.tiktok4j.enums.CommentActionType;
import com.github.zikifaker.tiktok4j.mapper.CommentMapper;
import com.github.zikifaker.tiktok4j.mq.message.DeleteCommentMessage;
import com.github.zikifaker.tiktok4j.service.CommentService;
import com.github.zikifaker.tiktok4j.service.UserService;
import com.github.zikifaker.tiktok4j.utils.SensitiveWordFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    private StringRedisTemplate cacheService;

    private UserService userService;

    private RocketMQTemplate mqService;

    private CommentMapper commentMapper;

    private SensitiveWordFilter sensitiveWordFilter;

    private Executor threadPool;

    private static final int CACHE_EXPIRE_DAYS = 30;

    @Autowired
    public CommentServiceImpl(
            StringRedisTemplate cacheService,
            UserService userService,
            RocketMQTemplate mqService,
            CommentMapper commentMapper,
            SensitiveWordFilter sensitiveWordFilter,
            @Qualifier("commentTaskExecutor") Executor threadPool
    ) {
        this.cacheService = cacheService;
        this.userService = userService;
        this.mqService = mqService;
        this.commentMapper = commentMapper;
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.threadPool = threadPool;
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
        String[] ids = commentIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
        cacheService.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
        cacheService.opsForSet().add(key, ids);

        Long count = cacheService.opsForSet().size(key);
        return count != null ? count : 0L;
    }

    @Override
    public HandleCommentResultBO handleComment(HandleCommentBO handleCommentBO) {
        CommentActionType type = handleCommentBO.getActionType();
        return switch (type) {
            case POST -> postComment(handleCommentBO);
            case DELETE -> deleteComment(handleCommentBO);
        };
    }

    private HandleCommentResultBO postComment(HandleCommentBO handleCommentBO) {
        // 过滤敏感词
        String filteredText = sensitiveWordFilter.filter(handleCommentBO.getText());

        // 持久化评论
        Comment comment = new Comment();
        BeanUtils.copyProperties(handleCommentBO, comment, "actionType");
        comment.setCreateTime(Instant.now().atZone(ZoneOffset.UTC).toLocalDateTime());
        comment.setContent(filteredText);
        commentMapper.save(comment);

        // 异步维护视频评论 id 缓存
        CompletableFuture.runAsync(() -> {
            String videoCommentsKey = String.format(RedisKeys.VIDEO_COMMENTS, handleCommentBO.getVideoId());
            if (Boolean.TRUE.equals(cacheService.hasKey(videoCommentsKey))) {
                cacheService.opsForSet().add(videoCommentsKey, String.valueOf(comment.getId()));
            } else {
                List<Long> commentIds = commentMapper.getCommentIds(handleCommentBO.getVideoId());
                String[] ids = commentIds.stream()
                        .map(String::valueOf)
                        .toArray(String[]::new);
                cacheService.expire(videoCommentsKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                cacheService.opsForSet().add(videoCommentsKey, ids);
            }
        }, threadPool);

        return HandleCommentResultBO.builder()
                .id(comment.getId())
                .user(userService.getUserInfo(handleCommentBO.getUserId(), handleCommentBO.getUserId()))
                .content(filteredText)
                .createTime(comment.getCreateTime())
                .build();
    }

    private HandleCommentResultBO deleteComment(HandleCommentBO handleCommentBO) {
        String videoCommentsKey = String.format(RedisKeys.VIDEO_COMMENTS, handleCommentBO.getVideoId());
        Long commentId = handleCommentBO.getCommentId();
        if (Boolean.TRUE.equals(cacheService.hasKey(videoCommentsKey))) {
            cacheService.opsForSet().remove(videoCommentsKey, String.valueOf(commentId));
            sendDeleteMessage(commentId);
        } else {
            commentMapper.deleteById(commentId);
        }
        return null;
    }

    private void sendDeleteMessage(Long commentId) {
        DeleteCommentMessage message = new DeleteCommentMessage(commentId);
        String destination = String.format("%s:%s", MQConstants.TOPIC_TIKTOK_COMMENT, MQConstants.TAG_DELETE);
        mqService.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("Sent delete comment message: commentId={}", commentId);
            }

            @Override
            public void onException(Throwable e) {
                log.error("Failed to send delete comment message: commentId={}, error={}", commentId, e.getMessage());
            }
        });
    }

    @Override
    public List<GetCommentListBO> getCommentList(Long videoId, Long userId) {
        List<Comment> comments = commentMapper.getCommentsByVideoId(videoId);

        // 异步组装 BO
        List<GetCommentListBO> commentList = comments.stream()
                .map(comment -> GetCommentListBO.builder()
                        .id(comment.getId())
                        .userInfo(userService.getUserInfo(userId, comment.getUserId()))
                        .content(comment.getContent())
                        .createTime(comment.getCreateTime())
                        .build())
                .toList();

        // 异步维护视频的评论 id 集合缓存
        if (!comments.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                String key = String.format(RedisKeys.VIDEO_COMMENTS, videoId);
                String[] ids = comments.stream()
                        .map(comment -> String.valueOf(comment.getId()))
                        .toArray(String[]::new);
                cacheService.opsForSet().add(key, ids);
                cacheService.expire(key, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            }, threadPool);
        }

        return commentList;
    }
}
