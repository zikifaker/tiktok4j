package com.github.zikifaker.tiktok4j.service;

import java.io.InputStream;

public interface OSSService {
    void uploadFile(InputStream input, String objectKey);

    InputStream getFile(String objectKey);
}