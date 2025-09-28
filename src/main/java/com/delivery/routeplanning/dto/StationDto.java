package com.delivery.routeplanning.dto;

public class StationDto {
    private String stationId;
    private String name;
    private Location location;
    private int availableDrones;
    private int availableRobots;

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public int getAvailableDrones() { return availableDrones; }
    public void setAvailableDrones(int availableDrones) { this.availableDrones = availableDrones; }
    public int getAvailableRobots() { return availableRobots; }
    public void setAvailableRobots(int availableRobots) { this.availableRobots = availableRobots; }
}