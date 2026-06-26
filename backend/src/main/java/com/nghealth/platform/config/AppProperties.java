package com.nghealth.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String hospitalName,
        String corsOrigins,
        boolean oauthEnabled,
        Storage storage,
        Fhir fhir
) {
    public record Storage(String localPath, boolean useS3) {}
    public record Fhir(String baseUrl) {}
}
