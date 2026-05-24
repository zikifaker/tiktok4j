package com.github.zikifaker.tiktok4j.utils;

import com.github.zikifaker.tiktok4j.consts.LikeActionType;
import org.springframework.core.convert.converter.Converter;

public class LikeActionTypeConverter implements Converter<Integer, LikeActionType> {
    @Override
    public LikeActionType convert(Integer source) {
        return LikeActionType.fromCode(source);
    }
}
