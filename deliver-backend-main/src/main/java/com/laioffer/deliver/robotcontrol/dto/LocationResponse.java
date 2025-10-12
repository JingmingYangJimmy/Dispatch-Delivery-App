package robotcontrol.dto;

public class LocationResponse {
    private String deviceId;
    private double lat;
    private double lng;
    private long timestamp;

    public LocationResponse() {}
    public LocationResponse(String deviceId, double lat, double lng, long timestamp) {
        this.deviceId = deviceId; this.lat = lat; this.lng = lng; this.timestamp = timestamp;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
