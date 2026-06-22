package com.github.zikifaker.tiktok4j.dto.resp.comment;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.zikifaker.tiktok4j.bo.GetCommentListBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GetCommentListResp {
    @JsonUnwrapped
    private BaseResponse resp;

    private List<GetCommentListBO> comments;
}
