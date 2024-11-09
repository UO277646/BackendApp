package com.work.demo.service.dto;

import com.work.demo.rest.dto.ObjectDetectionResult;
import lombok.*;

import java.awt.image.BufferedImage;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ObjetoImagen {
    private List<ObjectDetectionResult> objetos;
    private String image;
    private String fallos;
}
