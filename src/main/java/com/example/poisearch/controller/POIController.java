package com.example.poisearch.controller;

import com.example.poisearch.model.POISearchRequest;
import com.example.poisearch.model.POISearchResponse;
import com.example.poisearch.service.POISearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/poi")
@RequiredArgsConstructor
@Validated
public class POIController {

    private final POISearchService poiSearchService;

    @GetMapping("/search")
    public ResponseEntity<POISearchResponse> search(@Valid @ModelAttribute POISearchRequest request) {
        log.info("Received POI search request: {}", request);
        
        POISearchResponse response = poiSearchService.search(request);
        
        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
