package com.delivery.dto;

import java.util.List;

public class BatchPathRecommendationResponse {
    private List<PathRecommendationResponse> results;

    public List<PathRecommendationResponse> getResults() { return results; }
    public void setResults(List<PathRecommendationResponse> results) { this.results = results; }
}
