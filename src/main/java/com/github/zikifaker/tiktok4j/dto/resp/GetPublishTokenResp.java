package com.github.zikifaker.tiktok4j.dto.resp;

import com.github.zikifaker.tiktok4j.bo.GetPublishTokenBO;
import com.github.zikifaker.tiktok4j.consts.BaseResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetPublishTokenResp {
    private BaseResponse resp;
    private GetPublishTokenBO getUploadTokenBO;
}
