package com.delivery.service;

import com.delivery.dto.Location;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class GoogleMapsService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${google.maps.api.key}")
    private String apiKey;

    private static final String GEOCODING_API = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String DISTANCE_MATRIX_API = "https://maps.googleapis.com/maps/api/distancematrix/json";
    private static final String ROADS_API = "https://roads.googleapis.com/v1/nearestRoads";

    @Cacheable(value = "geocoding", key = "#address")
    public Location geocodeAddress(String address) {
        try {
            String url = String.format("%s?address=%s&key=%s",
                    GEOCODING_API,
                    address.replace(" ", "+"),
                    apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("OK".equals(root.get("status").asText())) {
                JsonNode location = root.get("results").get(0).get("geometry").get("location");
                Location loc = new Location();
                loc.setLat(location.get("lat").asDouble());
                loc.setLng(location.get("lng").asDouble());
                loc.setAddress(address);
                return loc;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> getDistanceMatrix(Location origin, Location destination, String mode) {
        try {
            String url = String.format("%s?origins=%f,%f&destinations=%f,%f&mode=%s&key=%s",
                    DISTANCE_MATRIX_API,
                    origin.getLat(), origin.getLng(),
                    destination.getLat(), destination.getLng(),
                    mode, apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("OK".equals(root.get("status").asText())) {
                JsonNode element = root.get("rows").get(0).get("elements").get(0);
                Map<String, Object> result = new HashMap<>();
                result.put("distance", element.get("distance").get("value").asInt());
                result.put("duration", element.get("duration").get("value").asInt());
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Location> getNearestRoads(Location location) {
        try {
            String url = String.format("%s?points=%f,%f&key=%s",
                    ROADS_API,
                    location.getLat(), location.getLng(),
                    apiKey);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<Location> roads = new ArrayList<>();
            JsonNode snappedPoints = root.get("snappedPoints");
            if (snappedPoints != null) {
                for (JsonNode point : snappedPoints) {
                    Location loc = new Location();
                    loc.setLat(point.get("location").get("latitude").asDouble());
                    loc.setLng(point.get("location").get("longitude").asDouble());
                    roads.add(loc);
                }
            }
            return roads;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
