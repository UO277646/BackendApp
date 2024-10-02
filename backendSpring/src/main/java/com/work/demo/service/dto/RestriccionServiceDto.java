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
public class RestriccionServiceDto {
    private Long idRestriccion; // CamelCase


    private Long proyectoId;

    private String objeto;

    private Date fechaDesde;

    private Date fechaHasta;

    private int cantidad;
}
