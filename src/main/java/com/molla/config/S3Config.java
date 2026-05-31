package com.molla.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(
            @Value("${aws.region}") String region,
            @Value("${aws.s3.credentials.access-key-id:}") String s3AccessKeyId,
            @Value("${aws.s3.credentials.secret-access-key:}") String s3SecretAccessKey,
            @Value("${aws.credentials.access-key-id:}") String accessKeyId,
            @Value("${aws.credentials.secret-access-key:}") String secretAccessKey,
            @Value("${aws.credentials.session-token:}") String sessionToken
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider(
                        s3AccessKeyId,
                        s3SecretAccessKey,
                        accessKeyId,
                        secretAccessKey,
                        sessionToken
                ))
                .build();
    }

    AwsCredentialsProvider credentialsProvider(
            String s3AccessKeyId,
            String s3SecretAccessKey,
            String accessKeyId,
            String secretAccessKey,
            String sessionToken
    ) {
        return credentialsProvider(
                selectCredential(s3AccessKeyId, accessKeyId),
                selectCredential(s3SecretAccessKey, secretAccessKey),
                sessionToken
        );
    }

    AwsCredentialsProvider credentialsProvider(String accessKeyId, String secretAccessKey, String sessionToken) {
        if (!StringUtils.hasText(accessKeyId) || !StringUtils.hasText(secretAccessKey)) {
            return DefaultCredentialsProvider.create();
        }

        if (StringUtils.hasText(sessionToken)) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKeyId, secretAccessKey, sessionToken)
            );
        }

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
    }

    String selectCredential(String preferredValue, String fallbackValue) {
        return StringUtils.hasText(preferredValue) ? preferredValue : fallbackValue;
    }
}
