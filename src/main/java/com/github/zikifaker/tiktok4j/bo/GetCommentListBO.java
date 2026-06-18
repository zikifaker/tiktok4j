package com.github.zikifaker.tiktok4j.bo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GetCommentListBO {
    private Long id;
    private UserInfoBO userInfo;
    private String content;
    private LocalDateTime createTime;
}
