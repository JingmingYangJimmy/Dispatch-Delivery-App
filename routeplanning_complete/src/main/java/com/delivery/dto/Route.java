package com.delivery.dto;

import java.util.List;

public class Route {
    private double distance;
    private int duration;
    private double cost;
    private List<Location> waypoints;

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public List<Location> getWaypoints() { return waypoints; }
    public void setWaypoints(List<Location> waypoints) { this.waypoints = waypoints; }
}
