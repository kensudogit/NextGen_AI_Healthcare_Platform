package com.nghealth.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.config.AwsProperties;
import com.nghealth.platform.config.OpenAiProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, AwsProperties.class, OpenAiProperties.class})
public class NgHealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(NgHealthApplication.class, args);
    }
}
