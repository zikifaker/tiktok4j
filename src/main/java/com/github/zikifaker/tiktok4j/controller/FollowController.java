package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.bo.UserInfoBO;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.dto.resp.follow.GetFollowerList;
import com.github.zikifaker.tiktok4j.dto.resp.follow.ToggleFollowResp;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import com.github.zikifaker.tiktok4j.enums.FollowActionType;
import com.github.zikifaker.tiktok4j.service.FollowService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/relation")
public class FollowController {
    private FollowService followService;

    @Autowired
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow/action")
    public ResponseEntity<ToggleFollowResp> toggleFollow(
            @RequestParam("user_id") Long userId,
            @RequestParam("action_type") FollowActionType actionType,
            HttpServletRequest request
    ) {
        Long currentUserId = (Long) request.getAttribute(ContextKeys.USER_ID);
        try {
            followService.toggleFollow(currentUserId, userId, actionType);
            ToggleFollowResp respBody = ToggleFollowResp.builder()
                    .resp(BaseResponse.SUCCESS)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (Exception e) {
            log.error("Error toggling follow: {}", e.getMessage());
            ToggleFollowResp respBody = ToggleFollowResp.builder()
                    .resp(BaseResponse.TOGGLE_FOLLOW_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }

    @GetMapping("/follow/list")
    public ResponseEntity<GetFollowerList> getFollowerList(@RequestParam("user_id") Long userId, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute(ContextKeys.USER_ID);
        try {
            List<UserInfoBO> followers = followService.getFollowers(currentUserId, userId);
            GetFollowerList respBody = GetFollowerList.builder()
                    .resp(BaseResponse.SUCCESS)
                    .userList(followers)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (Exception e) {
            log.error("Error getting follower list: {}", e.getMessage());
            GetFollowerList respBody = GetFollowerList.builder()
                    .resp(BaseResponse.GET_FOLLOWER_LIST_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }
}
