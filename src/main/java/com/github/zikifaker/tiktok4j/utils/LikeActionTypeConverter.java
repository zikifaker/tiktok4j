package com.github.zikifaker.tiktok4j.utils;

import com.github.zikifaker.tiktok4j.consts.LikeActionType;
import org.springframework.core.convert.converter.Converter;

public class LikeActionTypeConverter implements Converter<String, LikeActionType> {
    @Override
    public LikeActionType convert(String source) {
        return LikeActionType.fromCode(Integer.parseInt(source));
    }
}
