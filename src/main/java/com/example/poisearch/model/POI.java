package com.example.poisearch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POI {
    private String name;
    private String address;
    private Coordinates coordinates;
    private String type;
    private Double rating;
    private String placeId;
}
