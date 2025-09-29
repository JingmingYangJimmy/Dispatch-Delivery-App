package com.delivery.entity;
import javax.persistence.*;
@Entity
@Table(name = "stations")
public class Station {
    @Id
    private String stationId;
    private String name;
    private double latitude;
    private double longitude;
    private int availableDrones;
    private int availableRobots;

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getAvailableDrones() { return availableDrones; }
    public void setAvailableDrones(int availableDrones) { this.availableDrones = availableDrones; }
    public int getAvailableRobots() { return availableRobots; }
    public void setAvailableRobots(int availableRobots) { this.availableRobots = availableRobots; }
}
