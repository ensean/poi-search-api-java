package com.example.poisearch.service;

import com.example.poisearch.model.POI;
import com.example.poisearch.model.POISearchRequest;
import com.example.poisearch.model.POISearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class POISearchService {

    private final AWSLocationService awsLocationService;
    private final GoogleMapsService googleMapsService;

    public POISearchResponse search(POISearchRequest request) {
        try {
            // Try AWS Location Service first
            log.info("Attempting AWS Location Service search");
            List<POI> awsResults = awsLocationService.searchNearby(request);

            if (awsResults != null && !awsResults.isEmpty()) {
                log.info("AWS returned {} results", awsResults.size());
                return POISearchResponse.builder()
                        .success(true)
                        .source("aws")
                        .results(awsResults)
                        .count(awsResults.size())
                        .build();
            }

            // Fallback to Google Maps if AWS returned no results
            log.info("AWS returned no results, falling back to Google Maps");
            List<POI> googleResults = googleMapsService.searchNearby(request);

            return POISearchResponse.builder()
                    .success(true)
                    .source("google")
                    .results(googleResults)
                    .count(googleResults.size())
                    .build();

        } catch (Exception awsError) {
            // If AWS fails, try Google Maps as fallback
            log.error("AWS Location Service failed, falling back to Google Maps", awsError);

            try {
                List<POI> googleResults = googleMapsService.searchNearby(request);
                return POISearchResponse.builder()
                        .success(true)
                        .source("google")
                        .results(googleResults)
                        .count(googleResults.size())
                        .build();

            } catch (Exception googleError) {
                log.error("Both AWS and Google Maps failed", googleError);
                
                return POISearchResponse.builder()
                        .success(false)
                        .source(null)
                        .results(List.of())
                        .count(0)
                        .error("Failed to search POIs from both services")
                        .build();
            }
        }
    }
}
