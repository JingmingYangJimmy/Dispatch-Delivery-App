package com.delivery.routeplanning.dto;

public class Recommendation {
    private String type;
    private String device;
    private String deviceId;
    private Route route;
    private Score score;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public Score getScore() { return score; }
    public void setScore(Score score) { this.score = score; }
}
