package com.github.zikifaker.tiktok4j.utils;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.stereotype.Component;

@Component
public class SensitiveWordFilter {
    private SensitiveWordBs sensitiveWordBs;

    public SensitiveWordFilter() {
        this.sensitiveWordBs = SensitiveWordBs.newInstance()
                .ignoreCase(true)
                .ignoreWidth(true)
                .ignoreNumStyle(true)
                .ignoreChineseStyle(true)
                .enableNumCheck(false)
                .init();
    }

    public String filter(String text) {
        return sensitiveWordBs.replace(text);
    }
}
