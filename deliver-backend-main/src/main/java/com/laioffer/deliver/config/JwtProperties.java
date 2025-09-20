package com.laioffer.deliver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        int accessTtlMinutes,
        int refreshTtlDays
) {}
