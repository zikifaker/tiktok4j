package com.github.zikifaker.tiktok4j.dto.resp;

import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRegisterResp {
    private BaseResponse resp;
    private Long userId;
    private String token;
}
