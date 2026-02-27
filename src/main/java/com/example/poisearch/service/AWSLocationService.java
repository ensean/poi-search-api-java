package com.example.poisearch.service;

import com.example.poisearch.model.Coordinates;
import com.example.poisearch.model.POI;
import com.example.poisearch.model.POISearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.geoplaces.GeoPlacesClient;
import software.amazon.awssdk.services.geoplaces.model.SearchNearbyRequest;
import software.amazon.awssdk.services.geoplaces.model.SearchNearbyResponse;
import software.amazon.awssdk.services.geoplaces.model.SearchNearbyResultItem;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AWSLocationService {

    private final GeoPlacesClient geoPlacesClient;

    public List<POI> searchNearby(POISearchRequest request) {
        try {
            log.info("Searching AWS Geo Places with SearchNearby API (Places V2), params: {}", request);

            // Set radius (default to 1000 meters if not provided)
            Integer radius = request.getRadius() != null ? request.getRadius() : 1000;

            // Build SearchNearby request - NO Place Index required!
            SearchNearbyRequest.Builder requestBuilder = SearchNearbyRequest.builder()
                    .queryPosition(request.getLng(), request.getLat()) // AWS uses [lng, lat] order
                    .maxResults(50)
                    .queryRadius(radius.longValue()); // Radius in meters

            // Add filter by category if query is provided
            if (request.getQuery() != null && !request.getQuery().isEmpty()) {
                requestBuilder.filter(builder -> builder
                        .includeCategories(request.getQuery()) // Filter by category
                );
            }

            SearchNearbyRequest awsRequest = requestBuilder.build();
            SearchNearbyResponse response = geoPlacesClient.searchNearby(awsRequest);

            if (response.resultItems() == null || response.resultItems().isEmpty()) {
                log.info("No results from AWS Geo Places SearchNearby");
                return List.of();
            }

            List<POI> pois = response.resultItems().stream()
                    .map(this::convertToPOI)
                    .collect(Collectors.toList());

            log.info("AWS SearchNearby returned {} results", pois.size());
            return pois;

        } catch (Exception e) {
            log.error("AWS Geo Places SearchNearby error: {}", e.getMessage(), e);

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("AccessDeniedException")) {
                throw new RuntimeException(
                    "Access denied to AWS Geo Places. " +
                    "Please ensure IAM user/role has 'geo-places:SearchNearby' permission", e);
            }

            throw new RuntimeException("AWS Geo Places error: " + e.getMessage(), e);
        }
    }

    private POI convertToPOI(SearchNearbyResultItem item) {
        Double poiLat = item.position() != null && item.position().size() >= 2
                ? item.position().get(1) : null;
        Double poiLng = item.position() != null && item.position().size() >= 2
                ? item.position().get(0) : null;

        // Distance is provided directly by the API (convert Long to Double)
        Double distance = item.distance() != null ? item.distance().doubleValue() : null;

        return POI.builder()
                .name(item.title() != null ? item.title() : "Unknown")
                .address(formatAddress(item))
                .coordinates(poiLat != null && poiLng != null
                        ? new Coordinates(poiLat, poiLng)
                        : null)
                .type(item.placeType() != null ? item.placeType().toString() : null)
                .placeId(item.placeId())
                .distance(distance)
                .build();
    }

    private String formatAddress(SearchNearbyResultItem item) {
        if (item.address() != null) {
            var address = item.address();
            StringBuilder sb = new StringBuilder();

            if (address.label() != null) {
                return address.label();
            }

            if (address.street() != null) {
                sb.append(address.street()).append(", ");
            }
            if (address.locality() != null) sb.append(address.locality()).append(", ");
            if (address.region() != null) {
                var region = address.region();
                if (region.name() != null) {
                    sb.append(region.name()).append(", ");
                }
            }
            if (address.country() != null) {
                var country = address.country();
                if (country.name() != null) {
                    sb.append(country.name());
                }
            }

            String result = sb.toString();
            if (result.endsWith(", ")) {
                result = result.substring(0, result.length() - 2);
            }

            return !result.isEmpty() ? result : "Unknown";
        }

        return item.title() != null ? item.title() : "Unknown";
    }

}
