package com.example.poisearch.service;

import com.example.poisearch.model.Coordinates;
import com.example.poisearch.model.POI;
import com.example.poisearch.model.POISearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.location.LocationClient;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForPositionRequest;
import software.amazon.awssdk.services.location.model.SearchPlaceIndexForPositionResponse;
import software.amazon.awssdk.services.location.model.SearchForPositionResult;

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
            log.info("Searching AWS Location Service with params: {}", request);

            SearchPlaceIndexForPositionRequest awsRequest = SearchPlaceIndexForPositionRequest.builder()
                    .indexName(indexName)
                    .position(request.getLng(), request.getLat()) // AWS uses [lng, lat] order
                    .maxResults(50)
                    .build();

            SearchPlaceIndexForPositionResponse response = locationClient.searchPlaceIndexForPosition(awsRequest);

            if (response.results() == null || response.results().isEmpty()) {
                log.info("No results from AWS Location Service");
                return List.of();
            }

            List<POI> pois = response.results().stream()
                    .map(this::convertToPOI)
                    .collect(Collectors.toList());

            // Filter by radius if specified
            if (request.getRadius() != null) {
                pois = pois.stream()
                        .filter(poi -> calculateDistance(
                                request.getLat(),
                                request.getLng(),
                                poi.getCoordinates().getLat(),
                                poi.getCoordinates().getLng()
                        ) <= request.getRadius())
                        .collect(Collectors.toList());
            }

            // Filter by query if specified
            if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                String queryLower = request.getQuery().toLowerCase();
                pois = pois.stream()
                        .filter(poi -> 
                                poi.getName().toLowerCase().contains(queryLower) ||
                                poi.getAddress().toLowerCase().contains(queryLower) ||
                                (poi.getType() != null && poi.getType().toLowerCase().contains(queryLower))
                        )
                        .collect(Collectors.toList());
            }

            log.info("AWS returned {} results after filtering", pois.size());
            return pois;

        } catch (Exception e) {
            log.error("AWS Location Service error", e);
            throw new RuntimeException("AWS Location Service error: " + e.getMessage(), e);
        }
    }

    private POI convertToPOI(SearchForPositionResult result) {
        var place = result.place();
        
        return POI.builder()
                .name(place.label() != null ? place.label() : "Unknown")
                .address(formatAddress(place))
                .coordinates(new Coordinates(
                        place.geometry().point().get(1), // lat
                        place.geometry().point().get(0)  // lng
                ))
                .type(place.categories() != null && !place.categories().isEmpty() 
                        ? place.categories().get(0) 
                        : null)
                .build();
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
}
