package com.work.demo.rest.dto;


import com.work.demo.repository.Proyecto;
import lombok.*;

import java.sql.Date;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeteccionApiDto {

    private Long deteccionId; // CamelCase


    private Long proyectoId;


    private Date fotoId;

    private String objeto;

    private double x;

    private double y;

    private double weight;

    private double height;

    private double confidence;
}

