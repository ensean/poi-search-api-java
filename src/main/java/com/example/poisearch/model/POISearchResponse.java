package com.example.poisearch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POISearchResponse {
    private Boolean success;
    private String source; // "aws" or "google"
    private List<POI> results;
    private Integer count;
    private String error;
}
