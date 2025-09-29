package com.delivery.service;

import com.delivery.dto.*;
import com.delivery.entity.Device;
import com.delivery.entity.Station;
import com.delivery.repository.DeviceRepository;
import com.delivery.repository.StationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json";
    private static final String ROADS_API_URL = "https://roads.googleapis.com/v1/snapToRoads";
    private static final double DRONE_COST_PER_KM = 0.5;
    private static final double ROBOT_COST_PER_KM = 0.3;
    private static final double DRONE_BASE_COST = 2.0;
    private static final double ROBOT_BASE_COST = 1.5;

    public PathRecommendationResponse recommendPath(PathRecommendationRequest request) {
        List<Device> availableDevices = findAvailableDevices(request.getOrigin(), request.getPackageWeight());

        // 获取Google Maps路径信息
        GoogleMapsRoute droneRoute = getGoogleMapsRoute(request.getOrigin(), request.getDestination(), "DRONE");
        GoogleMapsRoute robotRoute = getGoogleMapsRoute(request.getOrigin(), request.getDestination(), "ROBOT");

        List<Recommendation> recommendations = new ArrayList<>();

        // 生成三种推荐方案
        recommendations.add(generateFastestRoute(request, availableDevices, droneRoute, robotRoute));
        recommendations.add(generateCheapestRoute(request, availableDevices, droneRoute, robotRoute));
        recommendations.add(generateOptimalRoute(request, availableDevices, droneRoute, robotRoute));

        PathRecommendationResponse response = new PathRecommendationResponse();
        response.setOrderId(request.getOrderId());
        response.setRecommendations(recommendations);
        return response;
    }

    private GoogleMapsRoute getGoogleMapsRoute(Location origin, Location destination, String deviceType) {
        try {
            String mode = "DRONE".equals(deviceType) ? "walking" : "driving";
            String avoid = "ROBOT".equals(deviceType) ? "highways" : "";

            String url = String.format("%s?origin=%f,%f&destination=%f,%f&mode=%s&avoid=%s&key=%s",
                    DIRECTIONS_API_URL,
                    origin.getLat(), origin.getLng(),
                    destination.getLat(), destination.getLng(),
                    mode, avoid, googleMapsApiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("OK".equals(root.get("status").asText())) {
                JsonNode route = root.get("routes").get(0);
                JsonNode leg = route.get("legs").get(0);

                GoogleMapsRoute gmRoute = new GoogleMapsRoute();
                gmRoute.setDistance(leg.get("distance").get("value").asDouble() / 1000.0);
                gmRoute.setDuration(leg.get("duration").get("value").asInt() / 60);

                // 提取航点
                List<Location> waypoints = new ArrayList<>();
                JsonNode steps = leg.get("steps");
                for (JsonNode step : steps) {
                    Location loc = new Location();
                    loc.setLat(step.get("end_location").get("lat").asDouble());
                    loc.setLng(step.get("end_location").get("lng").asDouble());
                    waypoints.add(loc);
                }
                gmRoute.setWaypoints(waypoints);

                // 对于无人机，使用直线距离的1.2倍作为飞行距离
                if ("DRONE".equals(deviceType)) {
                    double directDistance = calculateDistance(origin, destination);
                    gmRoute.setDistance(directDistance * 1.2);
                    gmRoute.setDuration((int)(directDistance * 2));
                    gmRoute.setWaypoints(generateDroneWaypoints(origin, destination));
                }

                return gmRoute;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 降级方案：使用计算的距离
        GoogleMapsRoute fallback = new GoogleMapsRoute();
        double distance = calculateDistance(origin, destination);
        fallback.setDistance(distance);
        fallback.setDuration("DRONE".equals(deviceType) ? (int)(distance * 2) : (int)(distance * 6));
        fallback.setWaypoints(new ArrayList<>());
        return fallback;
    }

    private List<Location> generateDroneWaypoints(Location origin, Location destination) {
        List<Location> waypoints = new ArrayList<>();
        // 生成无人机飞行路径的中间点（直线飞行）
        int numPoints = 5;
        for (int i = 1; i <= numPoints; i++) {
            double ratio = (double) i / (numPoints + 1);
            Location waypoint = new Location();
            waypoint.setLat(origin.getLat() + (destination.getLat() - origin.getLat()) * ratio);
            waypoint.setLng(origin.getLng() + (destination.getLng() - origin.getLng()) * ratio);
            waypoints.add(waypoint);
        }
        return waypoints;
    }

    private Recommendation generateFastestRoute(PathRecommendationRequest request, List<Device> devices,
                                                GoogleMapsRoute droneRoute, GoogleMapsRoute robotRoute) {
        Device drone = devices.stream()
                .filter(d -> "DRONE".equals(d.getType()))
                .filter(d -> d.getMaxWeight() >= request.getPackageWeight())
                .filter(d -> d.getBattery() >= calculateBatteryRequired("DRONE", droneRoute.getDistance(), request.getPackageWeight()))
                .findFirst()
                .orElse(null);

        if (drone != null) {
            return createRecommendation("FASTEST", "DRONE", drone.getDeviceId(), droneRoute);
        }

        Device robot = devices.stream()
                .filter(d -> "ROBOT".equals(d.getType()))
                .findFirst()
                .orElse(devices.get(0));

        return createRecommendation("FASTEST", "ROBOT", robot.getDeviceId(), robotRoute);
    }

    private Recommendation generateCheapestRoute(PathRecommendationRequest request, List<Device> devices,
                                                 GoogleMapsRoute droneRoute, GoogleMapsRoute robotRoute) {
        Device robot = devices.stream()
                .filter(d -> "ROBOT".equals(d.getType()))
                .filter(d -> d.getMaxWeight() >= request.getPackageWeight())
                .filter(d -> d.getBattery() >= calculateBatteryRequired("ROBOT", robotRoute.getDistance(), request.getPackageWeight()))
                .findFirst()
                .orElse(devices.get(0));

        return createRecommendation("CHEAPEST", "ROBOT", robot.getDeviceId(), robotRoute);
    }

    private Recommendation generateOptimalRoute(PathRecommendationRequest request, List<Device> devices,
                                                GoogleMapsRoute droneRoute, GoogleMapsRoute robotRoute) {
        String deviceType = selectOptimalDevice(request.getPackageWeight(), droneRoute.getDistance());
        Device device = devices.stream()
                .filter(d -> deviceType.equals(d.getType()))
                .filter(d -> d.getMaxWeight() >= request.getPackageWeight())
                .findFirst()
                .orElse(devices.get(0));

        GoogleMapsRoute selectedRoute = "DRONE".equals(deviceType) ? droneRoute : robotRoute;
        return createRecommendation("OPTIMAL", deviceType, device.getDeviceId(), selectedRoute);
    }

    private Recommendation createRecommendation(String type, String device, String deviceId, GoogleMapsRoute gmRoute) {
        Recommendation rec = new Recommendation();
        rec.setType(type);
        rec.setDevice(device);
        rec.setDeviceId(deviceId);

        Route route = new Route();
        route.setDistance(gmRoute.getDistance());
        route.setDuration(gmRoute.getDuration());
        route.setCost(calculateCost(device, gmRoute.getDistance()));
        route.setWaypoints(gmRoute.getWaypoints());
        rec.setRoute(route);

        Score score = new Score();
        score.setTimeScore(type.equals("FASTEST") ? 1.0 : 0.5);
        score.setCostScore(type.equals("CHEAPEST") ? 1.0 : 0.5);
        score.setTotalScore(score.getTimeScore() * 0.5 + score.getCostScore() * 0.5);
        rec.setScore(score);

        return rec;
    }

    private double calculateCost(String deviceType, double distance) {
        if ("DRONE".equals(deviceType)) {
            return distance * DRONE_COST_PER_KM + DRONE_BASE_COST;
        } else {
            return distance * ROBOT_COST_PER_KM + ROBOT_BASE_COST;
        }
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

    private String selectOptimalDevice(double weight, double distance) {
        if (weight > 5) return "ROBOT";
        if (weight <= 1 && distance > 3) return "DRONE";
        if (distance < 3) return "ROBOT";
        if (distance > 8) return "DRONE";
        return weight > 3 ? "ROBOT" : "DRONE";
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

    // 内部类：Google Maps路径数据
    private static class GoogleMapsRoute {
        private double distance;
        private int duration;
        private List<Location> waypoints;

        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public List<Location> getWaypoints() { return waypoints; }
        public void setWaypoints(List<Location> waypoints) { this.waypoints = waypoints; }
    }
}