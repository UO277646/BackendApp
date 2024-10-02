package com.work.demo.service.dto;

import com.work.demo.rest.dto.ObjectDetectionContainer;
import com.work.demo.rest.dto.ObjectDetectionResult;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AnalisisReturnDto {
    private List<ObjectDetectionResult> detecciones;
    private byte[] image;
}
