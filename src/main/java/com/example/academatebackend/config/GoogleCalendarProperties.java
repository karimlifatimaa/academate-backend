package com.example.academatebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("app.google")
public class GoogleCalendarProperties {
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String calendarId = "primary";
}
