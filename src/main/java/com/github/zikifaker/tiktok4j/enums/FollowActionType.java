package com.github.zikifaker.tiktok4j.enums;

public enum FollowActionType implements ActionType {
    FOLLOW(1),
    UNFOLLOW(2);

    private final Integer code;

    FollowActionType(Integer code) {
        this.code = code;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public static FollowActionType fromCode(Integer code) {
        return ActionType.fromCode(FollowActionType.class, code);
    }
}
