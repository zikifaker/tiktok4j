package com.github.zikifaker.tiktok4j.consts;

public enum LikeActionType {
    LIKE(1),
    UNLIKE(2);

    private final Integer code;

    LikeActionType(Integer code) {
        this.code = code;
    }

    public static LikeActionType fromCode(Integer code) {
        for (LikeActionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid LikeActionType code: " + code);
    }
}
