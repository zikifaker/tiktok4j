package com.github.zikifaker.tiktok4j.bo;

import com.github.zikifaker.tiktok4j.entity.User;
import com.github.zikifaker.tiktok4j.entity.Video;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoBO {
    private Video video;
    private User author;
    private Long likeCount;
    private Long commentCount;
    private Boolean isLike;
}
