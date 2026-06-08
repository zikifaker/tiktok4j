package com.github.zikifaker.tiktok4j.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BaseResponse {
    SUCCESS(0, ""),

    USER_ALREADY_EXISTS(100, "User already exists"),
    USER_PASSWORD_ERROR(101, "User password error"),

    EMPTY_FEED(200, "Empty feed"),
    PUBLISH_VIDEO_ERROR(201, "Publish video error"),

    GET_PUBLISH_TOKEN_ERROR(300, "Get publish token error"),

    TOGGLE_LIKE_ERROR(400, "Toggle like error"),
    GET_LIKE_LIST_ERROR(401, "Get like list error"),

    HANDLE_COMMENT_ERROR(500, "Handle comment error");

    private final Integer code;
    private final String message;
}
