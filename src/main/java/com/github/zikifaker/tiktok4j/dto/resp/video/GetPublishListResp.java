package com.github.zikifaker.tiktok4j.dto.resp.video;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetPublishListResp {
    @JsonUnwrapped
    private BaseResponse resp;

    private List<VideoBO> videos;
}
