package com.work.demo.rest.dto;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectDetectionResult {
    private float x;
    private float y;
    private float weight;
    private float height;

    private float confidence;
}
