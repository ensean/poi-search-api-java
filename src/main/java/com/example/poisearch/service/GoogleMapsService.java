package com.example.poisearch.service;

import com.example.poisearch.model.Coordinates;
import com.example.poisearch.model.POI;
import com.example.poisearch.model.POISearchRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMapsService {

    private final GeoApiContext geoApiContext;

    public List<POI> searchNearby(POISearchRequest request) {
        try {
            log.info("Searching Google Maps Places API with params: {}", request);

            LatLng location = new LatLng(request.getLat(), request.getLng());
            int radius = request.getRadius() != null ? request.getRadius() : 1000;

            PlacesSearchResponse response = PlacesApi.nearbySearchQuery(geoApiContext, location)
                    .radius(radius)
                    .keyword(request.getQuery())
                    .await();

            if (response.results == null || response.results.length == 0) {
                log.info("No results from Google Maps");
                return List.of();
            }

            List<POI> pois = Arrays.stream(response.results)
                    .map(this::convertToPOI)
                    .collect(Collectors.toList());

            log.info("Google Maps returned {} results", pois.size());
            return pois;

        } catch (Exception e) {
            log.error("Google Maps API error", e);
            throw new RuntimeException("Google Maps API error: " + e.getMessage(), e);
        }
    }

    private POI convertToPOI(PlacesSearchResult result) {
        return POI.builder()
                .name(result.name != null ? result.name : "Unknown")
                .address(result.vicinity != null ? result.vicinity : "Unknown")
                .coordinates(new Coordinates(
                        result.geometry.location.lat,
                        result.geometry.location.lng
                ))
                .type(result.types != null && result.types.length > 0
                        ? result.types[0]
                        : null)
                .rating(result.rating != 0 ? (double) result.rating : null)
                .placeId(result.placeId)
                .build();
    }
}
