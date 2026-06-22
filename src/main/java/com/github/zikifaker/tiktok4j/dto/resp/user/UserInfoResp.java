package com.github.zikifaker.tiktok4j.dto.resp.user;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoResp {
    @JsonUnwrapped
    private BaseResponse resp;

    private UserInfoBO userInfoBO;
}
