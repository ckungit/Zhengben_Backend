package com.zhangben.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * V24: Cloudflare R2 存储服务
 * 使用 AWS S3 SDK 流式上传文件到 R2
 */
@Service
public class R2StorageService {

    @Value("${r2.account-id}")
    private String accountId;

    @Value("${r2.access-key}")
    private String accessKey;

    @Value("${r2.secret-key}")
    private String secretKey;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    @Value("${avatar.max-file-size:51200}")
    private long maxFileSize; // 默认 50KB

    @Value("${avatar.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedTypes;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        // R2 的 endpoint 格式
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))  // R2 使用 auto region
                .forcePathStyle(true)       // R2 需要 path style
                .build();
    }

    /**
     * 上传头像图片（流式上传，节约内存）
     *
     * @param inputStream 图片输入流
     * @param contentType 图片类型
     * @param contentLength 文件大小
     * @param userId 用户ID
     * @return 图片的公开URL
     */
    public String uploadAvatar(InputStream inputStream, String contentType, long contentLength, Integer userId) {
        // 验证文件大小
        if (contentLength > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制（最大 " + (maxFileSize / 1024) + "KB）");
        }

        // 验证文件类型
        if (!isAllowedType(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持 JPEG、PNG、WebP");
        }

        // 生成唯一的文件名：avatars/{userId}/{uuid}.{ext}
        String extension = getExtensionFromContentType(contentType);
        String key = String.format("avatars/%d/%s.%s", userId, UUID.randomUUID().toString(), extension);

        // 流式上传到 R2
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .cacheControl("public, max-age=31536000") // 缓存1年
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));

        // 返回公开URL
        return publicUrl + "/" + key;
    }

    /**
     * 删除旧头像
     */
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }

        // 从URL提取key
        if (avatarUrl.startsWith(publicUrl)) {
            String key = avatarUrl.substring(publicUrl.length() + 1);
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteRequest);
            } catch (Exception e) {
                // 删除失败不影响主流程，仅记录日志
                System.err.println("删除旧头像失败: " + e.getMessage());
            }
        }
    }

    private boolean isAllowedType(String contentType) {
        if (contentType == null) return false;
        for (String type : allowedTypes.split(",")) {
            if (type.trim().equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }

    private String getExtensionFromContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
