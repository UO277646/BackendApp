package com.work.demo.repository;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
@Table(name="Restricciones")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Restriccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRestriccion; // CamelCase

    @ManyToOne
    @JoinColumn(name = "proyecto_id", nullable = false) // Mapeo explícito a proyecto_id
    private Proyecto proyecto;

    private String objeto;

    @Column(name = "fecha_desde") // Mapeo explícito de la columna fecha_desde
    private Date fechaDesde;

    @Column(name = "fecha_hasta") // Mapeo explícito de la columna fecha_hasta
    private Date fechaHasta;
    @Column(name = "cantidad_min") // Mapeo explícito de la columna fecha_hasta
    private int cantidadMin;

    @Column(name = "cantidad_max") // Mapeo explícito de la columna fecha_hasta
    private int cantidadMax;

    @Override
    public String toString () {
        return "Restriccion{" +
                "idRestriccion=" + idRestriccion +
                ", proyecto=" + proyecto +
                ", objeto='" + objeto + '\'' +
                ", fechaDesde=" + fechaDesde +
                ", fechaHasta=" + fechaHasta +
                ", cantidadMin=" + cantidadMin +
                ", cantidadMax=" + cantidadMax +
                ", cumplida=" + cumplida +
                '}';
    }
    @Column(name = "cumplida")
    private Boolean cumplida;
    @Column(name = "diaria")
    private Boolean diaria;
    @Column(name="borrado")
    private boolean borrado;
}
