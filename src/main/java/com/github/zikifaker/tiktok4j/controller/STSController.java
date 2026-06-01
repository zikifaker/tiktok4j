package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.dto.resp.GetPublishTokenResp;
import com.github.zikifaker.tiktok4j.service.STSService;
import com.github.zikifaker.tiktok4j.bo.GetPublishTokenBO;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class STSController {
    private STSService stsService;

    @Autowired
    public STSController(STSService stsService) {
        this.stsService = stsService;
    }

    @GetMapping("/publish/token")
    public ResponseEntity<GetPublishTokenResp> getPublishToken(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(ContextKeys.USER_ID);
        GetPublishTokenBO publishTokenBO = stsService.createPublishToken(userId);
        if (publishTokenBO == null) {
            GetPublishTokenResp resp = GetPublishTokenResp.builder()
                    .resp(BaseResponse.GET_PUBLISH_TOKEN_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(resp);
        }
        GetPublishTokenResp resp = GetPublishTokenResp.builder()
                .resp(BaseResponse.SUCCESS)
                .getUploadTokenBO(publishTokenBO)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resp);
    }
}
