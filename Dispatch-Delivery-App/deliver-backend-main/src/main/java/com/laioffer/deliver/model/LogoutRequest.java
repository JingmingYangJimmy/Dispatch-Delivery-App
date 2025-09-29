package com.laioffer.deliver.model;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String accessSid,
        String refreshToken
) {}
