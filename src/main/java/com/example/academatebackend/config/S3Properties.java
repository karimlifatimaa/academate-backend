package com.example.academatebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    private String bucketName;
    private String region;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private Duration presignedUrlExpiration = Duration.ofMinutes(15);
}
