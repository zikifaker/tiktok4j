package com.github.zikifaker.tiktok4j.bo;

import com.github.zikifaker.tiktok4j.enums.CommentActionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HandleCommentBO {
    private Long userId;
    private Long videoId;
    private String text;
    private Long commentId;
    private CommentActionType actionType;
}
