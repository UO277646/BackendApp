package com.work.demo.rest.dto;

import com.work.demo.repository.Proyecto;
import lombok.*;

import java.sql.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RestriccionApiDto {
    private Long idRestriccion; // CamelCase

    private String objeto;

    private Date fechaDesde;

    private Date fechaHasta;

    private int cantidad;

    private Long idProyecto;
}
