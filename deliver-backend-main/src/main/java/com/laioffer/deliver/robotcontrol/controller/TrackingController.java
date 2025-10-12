package robotcontrol.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import robotcontrol.dto.LocationRequest;
import robotcontrol.dto.LocationResponse;
import robotcontrol.dto.RemainingTimeResponse;
import robotcontrol.service.TrackingService;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@Validated
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateLocation(@Valid @RequestBody LocationRequest request) {
        trackingService.updateLocation(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/current")
    public ResponseEntity<LocationResponse> getCurrent(@RequestParam @NotBlank String deviceId) {
        return ResponseEntity.ok(trackingService.getCurrentLocation(deviceId));
    }

    @GetMapping("/remainingTime")
    public ResponseEntity<RemainingTimeResponse> getRemainingTime(@RequestParam @NotBlank String deviceId) {
        return ResponseEntity.ok(trackingService.getRemainingTime(deviceId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<LocationResponse>> getHistory(
            @RequestParam @NotBlank String deviceId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ResponseEntity.ok(trackingService.getLocationHistory(deviceId, startTime, endTime));
    }
}
