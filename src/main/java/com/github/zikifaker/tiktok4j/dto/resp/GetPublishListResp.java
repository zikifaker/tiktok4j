package com.github.zikifaker.tiktok4j.dto.resp;

import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetPublishListResp {
    private BaseResponse resp;
    private List<VideoBO> videos;
}
