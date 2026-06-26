package com.nghealth.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(String apiKey, String model) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
