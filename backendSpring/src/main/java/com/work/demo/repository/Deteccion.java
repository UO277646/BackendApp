package com.work.demo.repository;


import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;
@Table(name="Detecciones")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Deteccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deteccionId; // CamelCase

    @ManyToOne
    @JoinColumn(name = "proyecto_id", nullable = false) // Mapeo explícito a proyecto_id
    private Proyecto proyecto;

    @Column(name = "foto_id") // Mapeo explícito de la columna foto_id
    private Date fotoId;
    @Column(name = "objeto") // Mapeo explícito de la columna foto_id
    private String objeto;

    @Column(name = "x") // Mapeo explícito de la columna esquina1
    private double x;

    @Column(name = "y") // Mapeo explícito de la columna esquina2
    private double y;

    @Column(name = "weight") // Mapeo explícito de la columna esquina3
    private double weight;

    @Column(name = "height") // Mapeo explícito de la columna esquina4
    private double height;
    @Column(name = "confidence") // Mapeo explícito de la columna esquina4
    private double confidence;
}
