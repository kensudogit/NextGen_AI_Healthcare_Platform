package com.nghealth.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AppProperties appProperties;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    public SecurityConfig(AppProperties appProperties, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.appProperties = appProperties;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (appProperties.oauthEnabled() && issuerUri != null && !issuerUri.isBlank()) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/health", "/", "/actuator/health").permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                            .requestMatchers("/fhir/R4/metadata").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/phone/**").permitAll()
                            .requestMatchers("/api/hl7/**").hasAnyRole("STAFF", "ADMIN")
                            .requestMatchers("/api/**", "/fhir/**").authenticated()
                            .anyRequest().permitAll())
                    .oauth2ResourceServer(oauth -> oauth.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> patterns = new ArrayList<>(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.up.railway.app",
                "https://*.railway.app"
        ));
        Arrays.stream(appProperties.corsOrigins().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(patterns::add);
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
