package com.github.zikifaker.tiktok4j.enums;

public enum LikeActionType implements ActionType {
    LIKE(1),
    UNLIKE(2);

    private final Integer code;

    LikeActionType(Integer code) {
        this.code = code;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public static LikeActionType fromCode(Integer code) {
        return ActionType.fromCode(LikeActionType.class, code);
    }
}
