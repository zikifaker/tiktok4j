package com.github.zikifaker.tiktok4j.utils;

import com.github.zikifaker.tiktok4j.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
public class VideoUtils {
    private OSSService ossService;

    private static final int FFMPEG_EXEC_TIMEOUT = 60;

    @Autowired
    public VideoUtils(OSSService ossService) {
        this.ossService = ossService;
    }

    public Path extractAndUploadFirstFrame(String rawVideoObjectKey, String coverObjectKey) {
        Path coverFile = null;
        try {
            // 从 OSS 下载原视频
            InputStream input = ossService.getFile(rawVideoObjectKey);

            // 检查临时目录
            Path videoTempDir = Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    "tiktok4j",
                    "videos"
            );
            Path coverTempDir = Paths.get(
                    System.getProperty("java.io.tmpdir"),
                    "tiktok4j",
                    "covers"
            );
            Files.createDirectories(videoTempDir);
            Files.createDirectories(coverTempDir);

            // 将视频保存到系统临时目录
            Path videoFile = Files.createTempFile(videoTempDir, "video_", "");
            try (OutputStream out = Files.newOutputStream(videoFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            coverFile = Files.createTempFile(coverTempDir, "cover_", ".jpg");

            // 调用 FFmpeg 进程截取视频首帧作为封面
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", videoFile.toString(),
                    "-ss", "00:00:00.001",
                    "-vframes", "1",
                    coverFile.toString()
            );

            // 合并标准错误到标准输出，丢弃输出
            Process extractor = processBuilder
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            // 超时控制
            if (!extractor.waitFor(FFMPEG_EXEC_TIMEOUT, TimeUnit.SECONDS)) {
                extractor.destroyForcibly();
                throw new RuntimeException("FFmpeg first frame extraction timed out");
            }

            if (extractor.exitValue() != 0) {
                throw new RuntimeException("FFmpeg first frame extraction failed with code: " + extractor.exitValue());
            }

            // 上传封面至 OSS
            uploadFileToOSS(coverFile, coverObjectKey);

            return videoFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract and upload first frame: " + e.getMessage());
        } finally {
            deleteTempFile(coverFile);
        }
    }

    private void uploadFileToOSS(Path file, String objectKey) {
        try (InputStream in = Files.newInputStream(file)) {
            ossService.uploadFile(in, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to OSS: " + objectKey);
        }
    }

    private void deleteTempFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                log.error("Error deleting temp file {}: {}", file.getFileName(), e.getMessage());
            }
        }
    }

    public void convertAndUploadVideo(Path rawVideo, String hlsObjectKeyPrefix) {
        Path tempDir = Paths.get(
                System.getProperty("java.io.tmpdir"),
                "tiktok4j",
                "hls",
                UUID.randomUUID().toString()
        );

        Path m3u8File = tempDir.resolve("index.m3u8");

        try {
            Files.createDirectories(tempDir);

            // 调用 FFmpeg 进程将视频转为 HLS 格式
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", rawVideo.toString(),
                    "-c:v", "libx264",
                    "-c:a", "aac",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-hls_segment_filename", tempDir.resolve("%04d.ts").toString(),
                    m3u8File.toString()
            );

            Process converter = processBuilder
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!converter.waitFor(FFMPEG_EXEC_TIMEOUT, TimeUnit.SECONDS)) {
                converter.destroyForcibly();
                throw new RuntimeException("FFmpeg HLS transcoding timed out");
            }

            if (converter.exitValue() != 0) {
                throw new RuntimeException("FFmpeg HLS transcoding failed with code: " + converter.exitValue());
            }

            // 上传索引文件
            uploadFileToOSS(m3u8File, hlsObjectKeyPrefix + "/index.m3u8");

            // 上传切片文件
            try (Stream<Path> stream = Files.list(tempDir)) {
                stream.filter(path -> path.toString().endsWith(".ts"))
                        .forEach(ts -> {
                            String key = String.format("%s/%s", hlsObjectKeyPrefix, ts.getFileName());
                            uploadFileToOSS(ts, key);
                        });
            }

            // 删除临时目录
            deleteTempDir(tempDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert and upload video: " + e.getMessage());
        } finally {
            deleteTempFile(rawVideo);
        }
    }

    private void deleteTempDir(Path dir) {
        if (dir != null) {
            try {
                if (Files.exists(dir)) {
                    try (Stream<Path> stream = Files.list(dir)) {
                        stream.forEach(this::deleteTempFile);
                    }
                    Files.deleteIfExists(dir);
                }
            } catch (IOException e) {
                log.error("Error deleting temp dir {}: {}", dir.getFileName(), e.getMessage());
            }
        }
    }
}
