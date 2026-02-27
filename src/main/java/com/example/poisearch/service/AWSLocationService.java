package com.example.poisearch.service;

import com.example.poisearch.model.Coordinates;
import com.example.poisearch.model.POI;
import com.example.poisearch.model.POISearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextRequest;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForTextResponse;
import software.amazon.awssdk.services.location.model.SearchForTextResult;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AWSLocationService {

    private final LocationClient locationClient;

    @Value("${aws.location.index-name}")
    private String indexName;

    public List<POI> searchNearby(POISearchRequest request) {
        try {
            log.info("Searching AWS Location Service with SearchPlaceIndexForText API, params: {}", request);

            // Build search query - use query parameter or search for "places" near the location
            String searchText = request.getQuery() != null && !request.getQuery().isEmpty()
                    ? request.getQuery()
                    : "places";

            // Build SearchPlaceIndexForText request with bias position for nearby results
            SearchPlaceIndexForTextRequest awsRequest = SearchPlaceIndexForTextRequest.builder()
                    .indexName(indexName)
                    .text(searchText)
                    .biasPosition(request.getLng(), request.getLat()) // AWS uses [lng, lat] order
                    .maxResults(50)
                    .build();

            SearchPlaceIndexForTextResponse response = locationClient.searchPlaceIndexForText(awsRequest);

            if (response.results() == null || response.results().isEmpty()) {
                log.info("No results from AWS Location Service SearchPlaceIndexForText");
                return List.of();
            }

            // Convert and calculate distances
            Integer radius = request.getRadius() != null ? request.getRadius() : 1000;
            List<POI> pois = response.results().stream()
                    .map(result -> convertToPOI(result, request.getLat(), request.getLng()))
                    .filter(poi -> poi.getDistance() != null && poi.getDistance() <= radius) // Filter by radius
                    .collect(Collectors.toList());

            log.info("AWS SearchPlaceIndexForText returned {} results within {} meters", pois.size(), radius);
            return pois;

        } catch (Exception e) {
            log.error("AWS Location Service SearchPlaceIndexForText error", e);
            throw new RuntimeException("AWS Location Service error: " + e.getMessage(), e);
        }
    }

    private POI convertToPOI(SearchForTextResult result, double queryLat, double queryLng) {
        var place = result.place();
        double poiLat = place.geometry().point().get(1);
        double poiLng = place.geometry().point().get(0);

        // Calculate distance from query position
        double distance = calculateDistance(queryLat, queryLng, poiLat, poiLng);

        return POI.builder()
                .name(place.label() != null ? place.label() : "Unknown")
                .address(formatAddress(place))
                .coordinates(new Coordinates(poiLat, poiLng))
                .type(place.categories() != null && !place.categories().isEmpty()
                        ? place.categories().get(0)
                        : null)
                .placeId(result.placeId())
                .distance(distance)
                .build();
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371e3; // Earth radius in meters
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lng2 - lng1);

        double a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in meters
    }

    private String formatAddress(software.amazon.awssdk.services.location.model.Place place) {
        StringBuilder address = new StringBuilder();
        
        if (place.street() != null) address.append(place.street()).append(", ");
        if (place.municipality() != null) address.append(place.municipality()).append(", ");
        if (place.region() != null) address.append(place.region()).append(", ");
        if (place.country() != null) address.append(place.country());
        
        String result = address.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        
        return !result.isEmpty() ? result : (place.label() != null ? place.label() : "Unknown");
    }

}
