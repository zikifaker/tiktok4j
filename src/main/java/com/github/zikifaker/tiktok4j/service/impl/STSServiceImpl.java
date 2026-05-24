package com.github.zikifaker.tiktok4j.service.impl;

import com.aliyun.sts20150401.models.AssumeRoleResponse;
import com.aliyun.sts20150401.models.AssumeRoleResponseBody;
import com.github.zikifaker.tiktok4j.bo.GetPublishTokenBO;
import com.github.zikifaker.tiktok4j.config.OSSConfig;
import com.github.zikifaker.tiktok4j.service.STSService;
import com.aliyun.sts20150401.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.aliyun.sts20150401.models.AssumeRoleRequest;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@Slf4j
public class STSServiceImpl implements STSService {
    private Client client;

    private OSSConfig ossConfig;

    // token 过期时间（秒）
    private static final long EXPIRE_TIME = 360L;

    // 视频上传上限，设置为 1 GB
    private static final long MAX_VIDEO_SIZE = 1073741824L;

    @Autowired
    public STSServiceImpl(Client client, OSSConfig ossConfig) {
        this.client = client;
        this.ossConfig = ossConfig;
    }

    @Override
    public GetPublishTokenBO createPublishToken(Long userId) {
        AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials credential = getCredential();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Map<String, Object> policy = buildPolicy(credential, userId, now);

        ObjectMapper objectMapper = new ObjectMapper();

        String base64Policy;
        try {
            String policyJSON = objectMapper.writeValueAsString(policy);
            base64Policy = new String(Base64.encodeBase64(policyJSON.getBytes()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing policy to JSON: {}", e.getMessage());
            return null;
        }

        String signedPolicy = signPolicy(credential, base64Policy, now);
        if (signedPolicy == null) {
            return null;
        }

        String ossCredential = String.format("%s/%s/%s/oss/aliyun_v4_request",
                credential.accessKeyId,
                now.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                ossConfig.getRegion()
        );

        String objectKeyPrefix = String.format("videos/raw/%s/%d/", userId, now.toEpochSecond());

        GetPublishTokenBO publishTokenBO = GetPublishTokenBO.builder()
                .version("OSS4-HMAC-SHA256")
                .policy(base64Policy)
                .credential(ossCredential)
                .date(now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")))
                .signature(signedPolicy)
                .securityToken(credential.securityToken)
                .objectKeyPrefix(objectKeyPrefix)
                .host(String.format("https://%s.oss-%s.aliyuncs.com", ossConfig.getBucketName(), ossConfig.getRegion()))
                .build();
        return publishTokenBO;
    }

    private AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials getCredential() {
        AssumeRoleRequest req = new AssumeRoleRequest()
                .setRoleArn(ossConfig.getRoleARN())
                .setRoleSessionName("role_request_session");
        RuntimeOptions runtimeOptions = new RuntimeOptions();

        try {
            AssumeRoleResponse resp = client.assumeRoleWithOptions(req, runtimeOptions);
            return resp.body.credentials;
        } catch (Exception e) {
            log.error("Error getting credentials from aliyun STS Service: {}", e.getMessage());
        }

        return new AssumeRoleResponseBody
                .AssumeRoleResponseBodyCredentials()
                .setAccessKeyId("ERROR_ACCESS_KEY_ID")
                .setAccessKeySecret("ERROR_ACCESS_KEY_SECRET")
                .setSecurityToken("ERROR_SECURITY_TOKEN");
    }

    private Map<String, Object> buildPolicy(
            AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials credential,
            Long userId,
            ZonedDateTime now
    ) {
        Map<String, Object> policy = new HashMap<>();

        policy.put("expiration", generateExpiration(EXPIRE_TIME));

        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ossDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        long unixTimestamp = now.toEpochSecond();

        String ossCredential = String.format("%s/%s/%s/oss/aliyun_v4_request",
                credential.accessKeyId,
                date,
                ossConfig.getRegion()
        );
        String objectKeyPrefix = String.format("videos/raw/%s/%d/", userId, unixTimestamp);

        List<Object> conditions = Arrays.asList(
                Map.of("bucket", ossConfig.getBucketName()),
                Map.of("x-oss-security-token", credential.securityToken),
                Map.of("x-oss-signature-version", "OSS4-HMAC-SHA256"),
                Map.of("x-oss-credential", ossCredential),
                Map.of("x-oss-date", ossDate),
                Arrays.asList("content-length-range", 1, MAX_VIDEO_SIZE),
                Arrays.asList("eq", "$success_action_status", "200"),
                Arrays.asList("starts-with", "$key", objectKeyPrefix)
        );
        policy.put("conditions", conditions);

        return policy;
    }

    private String generateExpiration(long seconds) {
        Instant instant = Instant.now().plusSeconds(seconds);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }

    private String signPolicy(
            AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials credential,
            String base64Policy,
            ZonedDateTime now
    ) {
        try {
            byte[] key1 = hmacsha256(("aliyun_v4" + credential.accessKeySecret).getBytes(), now.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            byte[] key2 = hmacsha256(key1, ossConfig.getRegion());
            byte[] key3 = hmacsha256(key2, "oss");
            byte[] key4 = hmacsha256(key3, "aliyun_v4_request");
            byte[] result = hmacsha256(key4, base64Policy);
            return Hex.encodeHexString(result);
        } catch (RuntimeException e) {
            log.error("Error signing policy: {}", e.getMessage());
            return null;
        }
    }

    private byte[] hmacsha256(byte[] key, String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            return mac.doFinal(data.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
