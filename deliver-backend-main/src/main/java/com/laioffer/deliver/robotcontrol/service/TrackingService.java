package robotcontrol.service;

import robotcontrol.dto.LocationRequest;
import robotcontrol.dto.LocationResponse;
import robotcontrol.dto.RemainingTimeResponse;

import java.util.List;

public interface TrackingService {
    void updateLocation(LocationRequest request);
    LocationResponse getCurrentLocation(String deviceId);
    RemainingTimeResponse getRemainingTime(String deviceId);
    List<LocationResponse> getLocationHistory(String deviceId, String startTime, String endTime);
}
