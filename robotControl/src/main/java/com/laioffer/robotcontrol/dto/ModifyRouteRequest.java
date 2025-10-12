package com.laioffer.robotcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModifyRouteRequest(
        @NotNull RouteDTO newRoute,
        @NotBlank String routeId,
        @NotNull TrackingStatus status  // "in_transit" / "pending"（到站或送货途中）
) {}
