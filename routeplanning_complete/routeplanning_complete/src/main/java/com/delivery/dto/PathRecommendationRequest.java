package com.delivery.dto;

import java.time.LocalDateTime;
import java.util.List;

// PathRecommendationRequest.java
public class PathRecommendationRequest {
    private String orderId;
    private Location origin;
    private Location destination;
    private double packageWeight;
    private LocalDateTime deliveryTimeRequirement;

    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Location getOrigin() { return origin; }
    public void setOrigin(Location origin) { this.origin = origin; }
    public Location getDestination() { return destination; }
    public void setDestination(Location destination) { this.destination = destination; }
    public double getPackageWeight() { return packageWeight; }
    public void setPackageWeight(double packageWeight) { this.packageWeight = packageWeight; }
    public LocalDateTime getDeliveryTimeRequirement() { return deliveryTimeRequirement; }
    public void setDeliveryTimeRequirement(LocalDateTime deliveryTimeRequirement) { this.deliveryTimeRequirement = deliveryTimeRequirement; }
}