package com.work.demo.service.dto;


import com.work.demo.repository.Proyecto;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeteccionServiceDto {

    private Long deteccionId; // CamelCase


    private Proyecto proyecto;


    private Date fotoId;

    private String objeto;

    private int cantidad;

    private double esquina1;

    private double esquina2;

    private double esquina3;

    private double esquina4;

    private double accuracy;
}
