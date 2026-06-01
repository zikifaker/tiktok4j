package com.github.zikifaker.tiktok4j.enums;

public enum CommentActionType implements ActionType {
    POST(1),
    DELETE(2);

    private final Integer code;

    CommentActionType(Integer code) {
        this.code = code;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public static CommentActionType fromCode(Integer code) {
        return ActionType.fromCode(CommentActionType.class, code);
    }
}
