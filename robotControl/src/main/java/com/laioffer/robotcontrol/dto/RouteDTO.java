package com.laioffer.robotcontrol.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** 简化版路线描述；与 routeplanning 的 polyline 思路一致 */
public record RouteDTO(
        @NotEmpty List<LocationDTO> polyline,
        @NotNull Instant generatedAt,   // 生成时间（ISO8601）
        String source                   // 可选："ROUTING_SERVICE" 等
) {}
