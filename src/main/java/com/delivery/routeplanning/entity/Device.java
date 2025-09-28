package com.delivery.routeplanning.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "devices")
public class Device {
    @Id
    private String deviceId;
    private String type;
    private String stationId;
    private double latitude;
    private double longitude;
    private int battery;
    private double maxWeight;
    private String status;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }
    public double getMaxWeight() { return maxWeight; }
    public void setMaxWeight(double maxWeight) { this.maxWeight = maxWeight; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
