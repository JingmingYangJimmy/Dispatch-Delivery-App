package com.laioffer.robotcontrol.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record LocationDTO(
        @NotNull
        @DecimalMin("-90.0") @DecimalMax("90.0")
        @JsonAlias({"lat", "latitude"})
        Double latitude,

        @NotNull
        @DecimalMin("-180.0") @DecimalMax("180.0")
        @JsonAlias({"lng", "lon", "longitude"})
        Double longitude
) {}
