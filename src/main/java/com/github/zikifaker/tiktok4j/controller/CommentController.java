package com.github.zikifaker.tiktok4j.controller;

import com.github.zikifaker.tiktok4j.bo.GetCommentListBO;
import com.github.zikifaker.tiktok4j.bo.HandleCommentBO;
import com.github.zikifaker.tiktok4j.bo.HandleCommentResultBO;
import com.github.zikifaker.tiktok4j.consts.ContextKeys;
import com.github.zikifaker.tiktok4j.dto.resp.comment.GetCommentListResp;
import com.github.zikifaker.tiktok4j.dto.resp.comment.HandleCommentResp;
import com.github.zikifaker.tiktok4j.enums.BaseResponse;
import com.github.zikifaker.tiktok4j.enums.CommentActionType;
import com.github.zikifaker.tiktok4j.service.CommentService;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/comment")
public class CommentController {
    private CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<HandleCommentResp> handleComment(
            @RequestParam("video_id") Long videoId,
            @RequestParam("action_type") CommentActionType actionType,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "comment_id", required = false) Long commentId,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute(ContextKeys.USER_ID);
        HandleCommentBO handleCommentBO = HandleCommentBO.builder()
                .userId(userId)
                .videoId(videoId)
                .text(text)
                .commentId(commentId)
                .actionType(actionType)
                .build();

        try{
            HandleCommentResultBO result = commentService.handleComment(handleCommentBO);
            HandleCommentResp respBody = HandleCommentResp.builder()
                    .resp(BaseResponse.SUCCESS)
                    .comment(result)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (Exception e) {
            log.error("Error handling comment: {}", e.getMessage());
            HandleCommentResp respBody = HandleCommentResp.builder()
                    .resp(BaseResponse.HANDLE_COMMENT_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<GetCommentListResp> getCommentList(
            @RequestParam("video_id") Long videoId,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute(ContextKeys.USER_ID);
        try {
            List<GetCommentListBO> comments = commentService.getCommentList(videoId, userId);
            GetCommentListResp respBody = GetCommentListResp.builder()
                    .resp(BaseResponse.SUCCESS)
                    .comments(comments)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(respBody);
        } catch (Exception e) {
            log.error("Error getting comment list: {}", e.getMessage());
            GetCommentListResp respBody = GetCommentListResp.builder()
                    .resp(BaseResponse.GET_COMMENT_LIST_ERROR)
                    .build();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(respBody);
        }
    }
}
