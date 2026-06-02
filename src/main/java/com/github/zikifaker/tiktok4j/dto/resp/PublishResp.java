package com.github.zikifaker.tiktok4j.dto.resp;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublishResp {
    @JsonUnwrapped
    private BaseResponse resp;
}
