package com.work.demo.rest.dto;

import lombok.*;

import java.util.List;
@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectDetectionContainer {
    private List<ObjectDetectionResult> objects;
    private int quantity;
}
