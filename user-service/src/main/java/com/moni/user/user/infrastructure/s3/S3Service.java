package com.moni.user.user.infrastructure.s3;

import com.moni.common.error.exception.CustomException;
import com.moni.user.user.domain.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.region}")
    private String region;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public PresignedUrlResult generatePresignedUrl(UUID userId, String extension) {
        String key = "profiles/" + userId + "/" + UUID.randomUUID() + "." + extension;

        try {
            String mimeType = extension.equals("jpg") ? "image/jpeg" : "image/" + extension;
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(mimeType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            String presignedUrl = presigned.url().toString();
            String s3Url = "https://" + cloudfrontDomain + "/" + key;

            return new PresignedUrlResult(presignedUrl, s3Url);
        } catch (Exception e) {
            log.error("[S3] Presigned URL 생성 실패 - userId={}, extension={}", userId, extension, e);
            throw new CustomException(UserErrorCode.S3_PRESIGNED_URL_FAILED);
        }
    }

    public void deleteIfS3Url(String profileUrl) {
        if (profileUrl == null || !profileUrl.startsWith("https://" + cloudfrontDomain + "/")) {
            return;
        }
        String key = profileUrl.substring(("https://" + cloudfrontDomain + "/").length());
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("[S3] 기존 프로필 이미지 삭제 - key={}", key);
        } catch (Exception e) {
            log.warn("[S3] 기존 프로필 이미지 삭제 실패 - key={}", key, e);
        }
    }

    public record PresignedUrlResult(String presignedUrl, String s3Url) {
    }
}
