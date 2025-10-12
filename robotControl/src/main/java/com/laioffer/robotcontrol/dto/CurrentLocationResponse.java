package com.laioffer.robotcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CurrentLocationResponse(
        @NotBlank String deviceId,
        @NotNull LocationDTO location,
        @NotNull TrackingStatus status,
        @NotNull Instant timestamp
) {}
