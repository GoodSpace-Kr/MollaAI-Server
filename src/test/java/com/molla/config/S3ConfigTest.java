package com.molla.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private final S3Config s3Config = new S3Config();

    @Test
    void usesDedicatedS3CredentialsWhenProvided() {
        AwsCredentialsProvider provider = s3Config.credentialsProvider(
                "s3-access-key",
                "s3-secret-key",
                "default-access-key",
                "default-secret-key",
                null
        );

        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("s3-access-key");
        assertThat(credentials.secretAccessKey()).isEqualTo("s3-secret-key");
    }

    @Test
    void fallsBackToDefaultCredentialsWhenDedicatedS3CredentialsMissing() {
        AwsCredentialsProvider provider = s3Config.credentialsProvider(
                null,
                null,
                "default-access-key",
                "default-secret-key",
                null
        );

        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("default-access-key");
        assertThat(credentials.secretAccessKey()).isEqualTo("default-secret-key");
    }

    @Test
    void usesBasicCredentialsWhenAccessKeyAndSecretKeyProvided() {
        AwsCredentialsProvider provider = s3Config.credentialsProvider("access-key", "secret-key", null);

        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsBasicCredentials.class);
        assertThat(credentials.accessKeyId()).isEqualTo("access-key");
        assertThat(credentials.secretAccessKey()).isEqualTo("secret-key");
    }

    @Test
    void usesSessionCredentialsWhenSessionTokenProvided() {
        AwsCredentialsProvider provider = s3Config.credentialsProvider("access-key", "secret-key", "session-token");

        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials).isInstanceOf(AwsSessionCredentials.class);
        AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;
        assertThat(sessionCredentials.accessKeyId()).isEqualTo("access-key");
        assertThat(sessionCredentials.secretAccessKey()).isEqualTo("secret-key");
        assertThat(sessionCredentials.sessionToken()).isEqualTo("session-token");
    }

    @Test
    void fallsBackToDefaultProviderWhenCredentialsMissing() {
        AwsCredentialsProvider provider = s3Config.credentialsProvider(null, "secret-key", null);

        assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
    }
}
