package com.github.zikifaker.tiktok4j.mq.consumer;

import com.github.zikifaker.tiktok4j.consts.MQConstants;
import com.github.zikifaker.tiktok4j.mapper.CommentMapper;
import com.github.zikifaker.tiktok4j.mq.message.DeleteCommentMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = MQConstants.CG_TIKTOK_COMMENT,
        topic = MQConstants.TOPIC_TIKTOK_COMMENT,
        selectorExpression = MQConstants.TAG_DELETE
)
public class DeleteCommentConsumer implements RocketMQListener<DeleteCommentMessage> {
    private CommentMapper commentMapper;

    @Autowired
    public DeleteCommentConsumer(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public void onMessage(DeleteCommentMessage message) {
        log.info("Consumed delete comment message: commentId={}", message.getCommentId());
        try {
            commentMapper.deleteById(message.getCommentId());
        } catch (Exception e) {
            log.error("Failed to consume delete comment message: commentId={}, error={}",
                    message.getCommentId(),
                    e.getMessage()
            );
        }
    }
}
