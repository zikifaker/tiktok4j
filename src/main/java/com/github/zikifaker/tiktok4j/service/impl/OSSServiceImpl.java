package com.github.zikifaker.tiktok4j.service.impl;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.GetObjectResult;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import com.github.zikifaker.tiktok4j.config.OSSConfig;
import com.github.zikifaker.tiktok4j.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Slf4j
public class OSSServiceImpl implements OSSService {
    private OSSConfig ossConfig;

    private OSSClient ossClient;

    @Autowired
    public OSSServiceImpl(OSSConfig ossConfig, OSSClient ossClient) {
        this.ossConfig = ossConfig;
        this.ossClient = ossClient;
    }

    @Override
    public void uploadFile(InputStream input, String objectKey) {
        PutObjectRequest req = PutObjectRequest.newBuilder()
                .bucket(ossConfig.getBucketName())
                .key(objectKey)
                .body(BinaryData.fromStream(input))
                .build();
        ossClient.putObject(req);
    }

    @Override
    public InputStream getFile(String objectKey) {
        GetObjectRequest req = GetObjectRequest.newBuilder()
                .bucket(ossConfig.getBucketName())
                .key(objectKey)
                .build();
        GetObjectResult result = ossClient.getObject(req);
        return result.body();
    }
}
