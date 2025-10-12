package com.laioffer.robotcontrol.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record DeviceInfoResponse(
        @NotBlank String deviceId,
        @NotNull DeviceType deviceType,       // "robot" / "drone"
        @NotNull TrackingStatus status,       // tracking 运行时态
        @DecimalMin("0.0") Double availableDistanceKm,  // 等价续航；tracking.doc 中的 available_distance
        String routeId,
        String currentStation,                // 站点名称或ID（与 Station Management 对齐）
        @Min(0) @Max(100) Integer batteryLevel, // 若来自站点模块device表，可带上电量补充信息
        Instant startingTime                  // 任务开始时间
) {}
