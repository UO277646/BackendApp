package com.work.demo.rest.dto;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectDetectionResult {
    private double x;
    private double y;
    private double width;
    private double height;

    private double confidence;
    private String label;
}
