package com.example.academatebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("app.zoom")
public class ZoomProperties {
    private String accountId;
    private String clientId;
    private String clientSecret;
}
