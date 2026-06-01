package com.github.zikifaker.tiktok4j.enums;

public interface ActionType {
    Integer getCode();

    static <T extends ActionType> T fromCode(Class<T> c, Integer code) {
        if (!c.isEnum()) {
            throw new IllegalArgumentException(c + " must be an enum type");
        }
        for (T constant : c.getEnumConstants()) {
            if (constant.getCode().equals(code)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown %s code: %d", c.getSimpleName(), code));
    }
}
