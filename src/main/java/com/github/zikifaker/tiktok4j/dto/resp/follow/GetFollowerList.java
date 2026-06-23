package com.github.zikifaker.tiktok4j.dto.resp.follow;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetFollowerList {
    @JsonUnwrapped
    private BaseResponse resp;

    private List<UserInfoBO> userList;
}
