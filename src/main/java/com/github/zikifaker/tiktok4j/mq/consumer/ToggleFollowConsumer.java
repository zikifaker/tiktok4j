package com.github.zikifaker.tiktok4j.mq.consumer;

import com.github.zikifaker.tiktok4j.consts.MQConstants;
import com.github.zikifaker.tiktok4j.enums.FollowActionType;
import com.github.zikifaker.tiktok4j.mapper.FollowMapper;
import com.github.zikifaker.tiktok4j.mq.message.ToggleFollowMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        consumerGroup = MQConstants.CG_TIKTOK_FOLLOW,
        topic = MQConstants.TOPIC_TIKTOK_FOLLOW,
        selectorExpression = MQConstants.TAG_FOLLOW + " || " + MQConstants.TAG_UNFOLLOW
)
public class ToggleFollowConsumer implements RocketMQListener<ToggleFollowMessage> {
    private FollowMapper followMapper;

    @Autowired
    public ToggleFollowConsumer(FollowMapper followMapper) {
        this.followMapper = followMapper;
    }

    @Override
    public void onMessage(ToggleFollowMessage message) {
        log.info("Consumed toggle follow message: followerId={}, followeeId={}, action={}",
                message.getFollowerId(),
                message.getFolloweeId(),
                message.getActionType()
        );
        try {
            if (FollowActionType.FOLLOW.name().equals(message.getActionType())) {
                followMapper.follow(message.getFollowerId(), message.getFolloweeId());
            } else {
                followMapper.unfollow(message.getFollowerId(), message.getFolloweeId());
            }
        } catch (Exception e) {
            log.error("Failed to consume toggle follow message: followerId={}, followeeId={}, action={}, error={}",
                    message.getFollowerId(),
                    message.getFolloweeId(),
                    message.getActionType(),
                    e.getMessage()
            );
        }
    }
}
