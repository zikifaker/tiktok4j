package com.github.zikifaker.tiktok4j.utils;

import com.github.zikifaker.tiktok4j.enums.ActionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;

public class ActionTypeConverterFactory implements ConverterFactory<String, ActionType> {
    @Override
    public <T extends ActionType> @NonNull Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToActionTypeConverter<>(targetType);
    }

    private record StringToActionTypeConverter<T extends ActionType>(Class<T> c) implements Converter<String, T> {
        @Override
        public T convert(String source) {
            if (source.isBlank()) {
                throw new IllegalArgumentException("Source code cannot be blank");
            }
            return ActionType.fromCode(c, Integer.valueOf(source.trim()));
        }
    }
}
