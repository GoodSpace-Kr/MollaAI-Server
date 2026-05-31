package com.molla.domain.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
public class S3AudioUrlService {

    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration presignDuration;

    public S3AudioUrlService(
            S3Presigner s3Presigner,
            @Value("${aws.s3.audio-bucket}") String bucket,
            @Value("${aws.s3.presign-expiration-minutes:60}") long presignExpirationMinutes
    ) {
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.presignDuration = Duration.ofMinutes(presignExpirationMinutes);
    }

    public String createAudioUrl(String audioKey) {
        if (audioKey == null || audioKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(audioKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignDuration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
