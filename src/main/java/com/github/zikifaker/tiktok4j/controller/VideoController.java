package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.bo.PublishVideoBO;
import com.github.zikifaker.tiktok4j.bo.VideoBO;
import com.github.zikifaker.tiktok4j.consts.BaseResponse;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.dto.resp.FeedResp;
import com.github.zikifaker.tiktok4j.dto.resp.GetPublishListResp;
import com.github.zikifaker.tiktok4j.dto.resp.PublishResp;
import com.github.zikifaker.tiktok4j.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
public class VideoController {
    private VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/feed")
    public ResponseEntity<FeedResp> feed(@RequestParam(value = "latest_time", required = false) Long latestTime, HttpServletRequest request) {
        if (latestTime == null) {
            latestTime = System.currentTimeMillis() / 1000;
        }
        LocalDateTime lastTime = Instant
                .ofEpochSecond(latestTime)
                .atZone(ZoneOffset.UTC)
                .toLocalDateTime();

        Long userId = (Long) request.getAttribute(ContextKeys.USER_ID);
        if (userId == null) {
            userId = 0L;
        }

        List<VideoBO> videos = videoService.getFeed(userId, lastTime);
        if (videos.isEmpty()) {
            FeedResp respBody = FeedResp.builder()
                    .resp(BaseResponse.EMPTY_FEED)
                    .nextTime(System.currentTimeMillis() / 1000)
                    .videos(videos)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }

        Long nextTime = videos.get(videos.size() - 1)
                .getVideo()
                .getPublishTime()
                .toEpochSecond(ZoneOffset.UTC);

        FeedResp respBody = FeedResp.builder()
                .resp(BaseResponse.SUCCESS)
                .nextTime(nextTime)
                .videos(videos)
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(respBody);
    }

    @PostMapping("/publish")
    public ResponseEntity<PublishResp> publish(
            @RequestParam(value = "object_key") String objectKey,
            @RequestParam(value = "title") String title,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute(ContextKeys.USER_ID);
        PublishVideoBO publishVideoBO = PublishVideoBO.builder()
                .title(title)
                .userId(userId)
                .objectKey(objectKey)
                .build();

        try {
            videoService.publishVideo(publishVideoBO);
            PublishResp respBody = PublishResp.builder()
                    .resp(BaseResponse.SUCCESS)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (RuntimeException e) {
            PublishResp respBody = PublishResp.builder()
                    .resp(BaseResponse.PUBLISH_VIDEO_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }

    @GetMapping("publish/list")
    public ResponseEntity<GetPublishListResp> getPublishList(@RequestParam("user_id") Long userId) {
        GetPublishListResp respBody = GetPublishListResp.builder()
                .resp(BaseResponse.SUCCESS)
                .videos(videoService.getUserVideos(userId))
                .build();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(respBody);
    }
}
