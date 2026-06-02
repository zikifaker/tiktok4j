package com.github.zikifaker.tiktok4j.dto.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FeedResp {
    @JsonUnwrapped
    private BaseResponse resp;

    private Long nextTime;
    private List<VideoBO> videos;
}
