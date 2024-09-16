package com.work.demo.repository;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

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

    private int cantidad;
}
