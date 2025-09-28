package com.delivery.routeplanning.service;
import com.delivery.routeplanning.dto.*;
import com.delivery.routeplanning.entity.Device;
import com.delivery.routeplanning.entity.Station;
import com.delivery.routeplanning.repository.DeviceRepository;
import com.delivery.routeplanning.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PathRecommendationService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String GOOGLE_MAPS_API_KEY = "AIzaSyCrJcFLshN8sXH5sM1ak9sSD6FICr2gO6Y";
    private static final double DRONE_COST_PER_KM = 0.5;
    private static final double ROBOT_COST_PER_KM = 0.3;
    private static final double DRONE_BASE_COST = 2.0;
    private static final double ROBOT_BASE_COST = 1.5;

    public PathRecommendationResponse recommendPath(PathRecommendationRequest request) {
        List<Device> availableDevices = findAvailableDevices(request.getOrigin(), request.getPackageWeight());
        List<Recommendation> recommendations = new ArrayList<>();

        double distance = calculateDistance(request.getOrigin(), request.getDestination());

        // Generate three recommendations
        recommendations.add(generateFastestRoute(request, availableDevices, distance));
        recommendations.add(generateCheapestRoute(request, availableDevices, distance));
        recommendations.add(generateOptimalRoute(request, availableDevices, distance));

        PathRecommendationResponse response = new PathRecommendationResponse();
        response.setOrderId(request.getOrderId());
        response.setRecommendations(recommendations);
        return response;
    }

    public BatchPathRecommendationResponse recommendBatchPaths(BatchPathRecommendationRequest request) {
        BatchPathRecommendationResponse response = new BatchPathRecommendationResponse();
        List<PathRecommendationResponse> results = new ArrayList<>();

        for (OrderRequest order : request.getOrders()) {
            PathRecommendationRequest pathRequest = new PathRecommendationRequest();
            pathRequest.setOrderId(order.getOrderId());
            pathRequest.setOrigin(order.getOrigin());
            pathRequest.setDestination(order.getDestination());
            pathRequest.setPackageWeight(order.getPackageWeight());

            results.add(recommendPath(pathRequest));
        }

        response.setResults(results);
        return response;
    }

    private Recommendation generateFastestRoute(PathRecommendationRequest request, List<Device> devices, double distance) {
        Device drone = devices.stream()
                .filter(d -> "DRONE".equals(d.getType()))
                .filter(d -> d.getMaxWeight() >= request.getPackageWeight())
                .filter(d -> d.getBattery() >= calculateBatteryRequired("DRONE", distance, request.getPackageWeight()))
                .findFirst()
                .orElse(null);

        if (drone != null) {
            return createRecommendation("FASTEST", "DRONE", drone.getDeviceId(), distance,
                    (int)(distance * 2), distance * DRONE_COST_PER_KM + DRONE_BASE_COST);
        }

        Device robot = devices.stream()
                .filter(d -> "ROBOT".equals(d.getType()))
                .findFirst()
                .orElse(devices.get(0));

        return createRecommendation("FASTEST", "ROBOT", robot.getDeviceId(), distance,
                (int)(distance * 6), distance * ROBOT_COST_PER_KM + ROBOT_BASE_COST);
    }

    private Recommendation generateCheapestRoute(PathRecommendationRequest request, List<Device> devices, double distance) {
        Device robot = devices.stream()
                .filter(d -> "ROBOT".equals(d.getType()))
                .filter(d -> d.getMaxWeight() >= request.getPackageWeight())
                .filter(d -> d.getBattery() >= calculateBatteryRequired("ROBOT", distance, request.getPackageWeight()))
                .findFirst()
                .orElse(devices.get(0));

        return createRecommendation("CHEAPEST", "ROBOT", robot.getDeviceId(), distance,
                (int)(distance * 6), distance * ROBOT_COST_PER_KM + ROBOT_BASE_COST);
    }

    private Recommendation generateOptimalRoute(PathRecommendationRequest request, List<Device> devices, double distance) {
        double w1 = 0.5, w2 = 0.5;

        String deviceType = selectOptimalDevice(request.getPackageWeight(), distance);
        Device device = devices.stream()
                .filter(d -> deviceType.equals(d.getType()))
                .filter(d -> d.getMaxWeight() >= request.getPackageWeight())
                .findFirst()
                .orElse(devices.get(0));

        double cost = "DRONE".equals(deviceType) ?
                distance * DRONE_COST_PER_KM + DRONE_BASE_COST :
                distance * ROBOT_COST_PER_KM + ROBOT_BASE_COST;
        int duration = "DRONE".equals(deviceType) ? (int)(distance * 2) : (int)(distance * 6);

        return createRecommendation("OPTIMAL", deviceType, device.getDeviceId(), distance, duration, cost);
    }

    private String selectOptimalDevice(double weight, double distance) {
        if (weight > 5) return "ROBOT";
        if (weight <= 1 && distance > 3) return "DRONE";
        if (distance < 3) return "ROBOT";
        if (distance > 8) return "DRONE";
        return weight > 3 ? "ROBOT" : "DRONE";
    }

    private Recommendation createRecommendation(String type, String device, String deviceId,
                                                double distance, int duration, double cost) {
        Recommendation rec = new Recommendation();
        rec.setType(type);
        rec.setDevice(device);
        rec.setDeviceId(deviceId);

        Route route = new Route();
        route.setDistance(distance);
        route.setDuration(duration);
        route.setCost(cost);
        route.setWaypoints(new ArrayList<>());
        rec.setRoute(route);

        Score score = new Score();
        score.setTimeScore(type.equals("FASTEST") ? 1.0 : 0.5);
        score.setCostScore(type.equals("CHEAPEST") ? 1.0 : 0.5);
        score.setTotalScore(score.getTimeScore() * 0.5 + score.getCostScore() * 0.5);
        rec.setScore(score);

        return rec;
    }

    private List<Device> findAvailableDevices(Location origin, double weight) {
        return deviceRepository.findAvailableDevicesNearLocation(origin.getLat(), origin.getLng(), 10);
    }

    private double calculateDistance(Location origin, Location destination) {
        double lat1 = Math.toRadians(origin.getLat());
        double lat2 = Math.toRadians(destination.getLat());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(destination.getLng() - origin.getLng());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c;
    }

    private double calculateBatteryRequired(String deviceType, double distance, double weight) {
        if ("DRONE".equals(deviceType)) {
            return distance * 10 + weight * 2;
        } else {
            return distance * 5 + weight * 1;
        }
    }
}
