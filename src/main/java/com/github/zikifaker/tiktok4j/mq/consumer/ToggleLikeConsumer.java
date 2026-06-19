package com.github.zikifaker.tiktok4j.mq.consumer;

import com.github.zikifaker.tiktok4j.enums.LikeActionType;
import com.github.zikifaker.tiktok4j.consts.MQConstants;
import com.github.zikifaker.tiktok4j.mapper.LikeMapper;
import com.github.zikifaker.tiktok4j.mq.message.ToggleLikeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = MQConstants.CG_TIKTOK_LIKE,
        topic = MQConstants.TOPIC_TIKTOK_LIKE,
        selectorExpression = MQConstants.TAG_LIKE + " || " + MQConstants.TAG_UNLIKE
)
public class ToggleLikeConsumer implements RocketMQListener<ToggleLikeMessage> {
    private LikeMapper likeMapper;

    @Autowired
    public ToggleLikeConsumer(LikeMapper likeMapper) {
        this.likeMapper = likeMapper;
    }

    @Override
    public void onMessage(ToggleLikeMessage message) {
        log.info("Consumed toggle like message: userId={}, videoId={}, action={}",
                message.getUserId(),
                message.getVideoId(),
                message.getActionType()
        );
        try {
            Integer cancel = LikeActionType.LIKE.name().equals(message.getActionType()) ? 0 : 1;
            likeMapper.upsertLike(message.getUserId(), message.getVideoId(), cancel);
        } catch (Exception e) {
            log.error("Failed to consume toggle like message: userId={}, videoId={}, action={}, error={}",
                    message.getUserId(),
                    message.getVideoId(),
                    message.getActionType(),
                    e.getMessage()
            );
        }
    }
}
