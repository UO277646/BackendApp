package com.work.demo.service.dto;

import com.work.demo.rest.dto.ObjectDetectionContainer;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AnalisisReturnDto {
    private List<ObjectDetectionContainer> detecciones;
    private byte[] image;
}
