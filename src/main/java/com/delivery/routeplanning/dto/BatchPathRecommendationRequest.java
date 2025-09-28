package com.delivery.routeplanning.dto;

import java.util.List;

public class BatchPathRecommendationRequest {
    private List<OrderRequest> orders;

    public List<OrderRequest> getOrders() { return orders; }
    public void setOrders(List<OrderRequest> orders) { this.orders = orders; }
}
