package robotcontrol.dto;

public class RemainingTimeResponse {
    private String deviceId;
    private long secondsRemaining;
    private String eta; // ISO-8601

    public RemainingTimeResponse() {}
    public RemainingTimeResponse(String deviceId, long secondsRemaining, String eta) {
        this.deviceId = deviceId; this.secondsRemaining = secondsRemaining; this.eta = eta;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public long getSecondsRemaining() { return secondsRemaining; }
    public void setSecondsRemaining(long secondsRemaining) { this.secondsRemaining = secondsRemaining; }
    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }
}
