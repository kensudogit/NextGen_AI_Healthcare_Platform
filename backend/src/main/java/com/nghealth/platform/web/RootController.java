package com.nghealth.platform.web;

import com.nghealth.platform.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    private final AppProperties appProperties;

    public RootController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "hospital", appProperties.hospitalName());
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "name", "NextGen AI Healthcare Platform",
                "docs", "/swagger-ui.html",
                "fhir", "/fhir/R4/metadata",
                "dashboard", "/api/dashboard/stats");
    }
}
