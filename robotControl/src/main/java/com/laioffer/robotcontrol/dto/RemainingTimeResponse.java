package com.laioffer.robotcontrol.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RemainingTimeResponse(
        @NotBlank String deviceId,
        @Min(0) long remainingTimeSec
) {}
