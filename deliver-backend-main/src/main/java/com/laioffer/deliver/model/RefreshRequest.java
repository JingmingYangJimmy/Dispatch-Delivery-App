package com.laioffer.deliver.model;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken
) {}