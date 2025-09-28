package com.delivery.routeplanning.dto;
import java.time.LocalDateTime;
import java.util.List;

public class PathRecommendationResponse {
    private String orderId;
    private List<Recommendation> recommendations;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }
}
