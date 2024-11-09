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


    private Long proyectoId;

    private String objeto;

    private Date fechaDesde;

    private Date fechaHasta;


    private int cantidadMin;
    private int cantidadMax;
    private Boolean cumplida;
    private Boolean diaria;

    @Override
    public String toString () {
        return "RestriccionApiDto{" +
                "idRestriccion=" + idRestriccion +
                ", proyectoId=" + proyectoId +
                ", objeto='" + objeto + '\'' +
                ", fechaDesde=" + fechaDesde +
                ", fechaHasta=" + fechaHasta +
                ", cantidadMin=" + cantidadMin +
                ", cantidadMax=" + cantidadMax +
                ", cumplida=" + cumplida +
                "diaria="+diaria+
                '}';
    }
}
