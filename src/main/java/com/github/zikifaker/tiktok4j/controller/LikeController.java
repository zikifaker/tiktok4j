package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.enums.LikeActionType;
import com.github.zikifaker.tiktok4j.dto.resp.like.GetLikeListResp;
import com.github.zikifaker.tiktok4j.dto.resp.like.ToggleLikeResp;
import com.github.zikifaker.tiktok4j.service.LikeService;
import com.github.zikifaker.tiktok4j.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/like")
public class LikeController {
    private LikeService likeService;

    private VideoService videoService;

    @Autowired
    public LikeController(LikeService likeService, VideoService videoService) {
        this.likeService = likeService;
        this.videoService = videoService;
    }

    @PostMapping
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

    @GetMapping("/list")
    public ResponseEntity<GetLikeListResp> getLikeList(@RequestParam("user_id") Long userId, HttpServletRequest request){
        Long curUserId = (Long) request.getAttribute(ContextKeys.USER_ID);
        try {
            GetLikeListResp respBody = GetLikeListResp.builder()
                    .resp(BaseResponse.SUCCESS)
                    .videos(videoService.getUserLikeVideos(curUserId, userId))
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (Exception e) {
            log.error("Error getting like list: {}", e.getMessage());
            GetLikeListResp respBody = GetLikeListResp.builder()
                    .resp(BaseResponse.GET_LIKE_LIST_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }
}
