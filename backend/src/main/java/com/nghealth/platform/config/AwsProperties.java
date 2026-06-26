package com.nghealth.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aws")
public record AwsProperties(
        String region,
        String s3Bucket,
        String endpoint,
        String accessKey,
        String secretKey
) {}
