package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.dto.resp.ToggleFollowResp;
import com.github.zikifaker.tiktok4j.enums.FollowActionType;
import com.github.zikifaker.tiktok4j.service.FollowService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/relation")
public class FollowController {
    private FollowService followService;

    @Autowired
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/action")
    public ResponseEntity<ToggleFollowResp> toggleFollow(
            @RequestParam("user_id") Long userId,
            @RequestParam("action_type") FollowActionType actionType,
            HttpServletRequest request
    ) {
        Long currentUserId = (Long) request.getAttribute(ContextKeys.USER_ID);
        followService.toggleFollow(currentUserId, userId, actionType);
        return null;
    }
}
