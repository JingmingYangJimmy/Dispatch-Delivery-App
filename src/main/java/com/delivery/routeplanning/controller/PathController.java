package com.delivery.routeplanning.controller;

import com.delivery.routeplanning.dto.*;
import com.delivery.routeplanning.service.PathRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/path")
@CrossOrigin(origins = "*")
public class PathController {

    @Autowired
    private PathRecommendationService pathService;

    @PostMapping("/recommend")
    public PathRecommendationResponse recommend(@RequestBody PathRecommendationRequest request) {
        return pathService.recommendPath(request);
    }

    @PostMapping("/recommend/batch")
    public BatchPathRecommendationResponse recommendBatch(@RequestBody BatchPathRecommendationRequest request) {
        return pathService.recommendBatchPaths(request);
    }
}