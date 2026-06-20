package com.github.zikifaker.tiktok4j.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleFollowMessage {
    private Long followerId;
    private Long followeeId;
    private String actionType;
}
