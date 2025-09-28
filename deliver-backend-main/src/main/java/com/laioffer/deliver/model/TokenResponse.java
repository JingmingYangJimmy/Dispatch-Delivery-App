package com.laioffer.deliver.model;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String accessSid
) {}
