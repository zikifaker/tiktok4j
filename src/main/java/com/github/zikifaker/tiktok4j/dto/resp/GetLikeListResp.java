package com.github.zikifaker.tiktok4j.dto.resp;

import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.consts.BaseResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetLikeListResp {
    private BaseResponse resp;
    private List<VideoBO> videos;
}
