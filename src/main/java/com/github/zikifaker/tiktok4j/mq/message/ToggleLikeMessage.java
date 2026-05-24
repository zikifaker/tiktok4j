package com.github.zikifaker.tiktok4j.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleLikeMessage {
    private Long userId;
    private Long videoId;
    private String actionType;
}
