package com.delivery.routeplanning.dto;

public class OrderRequest {
    private String orderId;
    private Location origin;
    private Location destination;
    private double packageWeight;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Location getOrigin() { return origin; }
    public void setOrigin(Location origin) { this.origin = origin; }
    public Location getDestination() { return destination; }
    public void setDestination(Location destination) { this.destination = destination; }
    public double getPackageWeight() { return packageWeight; }
    public void setPackageWeight(double packageWeight) { this.packageWeight = packageWeight; }
}