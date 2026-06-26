package com.nghealth.platform.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String raw = env.getProperty("DATABASE_URL");
        if (raw != null && !raw.isBlank() && !raw.startsWith("jdbc:")) {
            DataSourceProperties props = new DataSourceProperties();
            parsePostgresUrl(raw, props);
            return props.initializeDataSourceBuilder().build();
        }
        String jdbcUrl = env.getProperty("DATABASE_JDBC_URL");
        if ((jdbcUrl == null || jdbcUrl.isBlank()) && raw != null && raw.startsWith("jdbc:")) {
            jdbcUrl = raw;
        }
        return DataSourceBuilder.create()
                .url(jdbcUrl != null ? jdbcUrl : env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5436/nghealth"))
                .username(env.getProperty("DATABASE_USER", env.getProperty("spring.datasource.username", "nghealth")))
                .password(env.getProperty("DATABASE_PASSWORD", env.getProperty("spring.datasource.password", "nghealth_dev")))
                .build();
    }

    private static void parsePostgresUrl(String url, DataSourceProperties props) {
        try {
            String normalized = url.replace("postgres://", "postgresql://");
            URI uri = URI.create(normalized);
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                props.setUsername(URLDecoder.decode(parts[0], StandardCharsets.UTF_8));
                if (parts.length > 1) {
                    props.setPassword(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() != null ? uri.getPath() : "/nghealth";
            props.setUrl("jdbc:postgresql://" + uri.getHost() + ":" + port + path);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: " + url, e);
        }
    }
}
