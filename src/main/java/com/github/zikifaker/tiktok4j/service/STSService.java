package com.github.zikifaker.tiktok4j.service;

import com.github.zikifaker.tiktok4j.bo.GetPublishTokenBO;

public interface STSService {
    GetPublishTokenBO createPublishToken(Long userId);
}
