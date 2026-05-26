package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.consts.BaseResponse;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.consts.LikeActionType;
import com.github.zikifaker.tiktok4j.dto.resp.ToggleLikeResp;
import com.github.zikifaker.tiktok4j.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/like")
public class LikeController {
    private LikeService likeService;

    @Autowired
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping()
    public ResponseEntity<ToggleLikeResp> toggleLike(
            @RequestParam("video_id") Long videoId,
            @RequestParam("action_type") LikeActionType actionType,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute(ContextKeys.USER_ID);
        try {
            likeService.toggleLike(userId, videoId, actionType);
            ToggleLikeResp respBody = ToggleLikeResp.builder()
                    .resp(BaseResponse.SUCCESS)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (Exception e) {
            log.error("Error toggling like: {}", e.getMessage());
            ToggleLikeResp respBody = ToggleLikeResp.builder()
                    .resp(BaseResponse.TOGGLE_LIKE_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }
}
