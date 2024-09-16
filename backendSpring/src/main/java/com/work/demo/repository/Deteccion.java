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
public class Deteccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deteccionId; // CamelCase

    @ManyToOne
    @JoinColumn(name = "proyecto_id", nullable = false) // Mapeo explícito a proyecto_id
    private Proyecto proyecto;

    @Column(name = "foto_id") // Mapeo explícito de la columna foto_id
    private Date fotoId;

    private String objeto;

    private int cantidad;

    @Column(name = "esquina1") // Mapeo explícito de la columna esquina1
    private double esquina1;

    @Column(name = "esquina2") // Mapeo explícito de la columna esquina2
    private double esquina2;

    @Column(name = "esquina3") // Mapeo explícito de la columna esquina3
    private double esquina3;

    @Column(name = "esquina4") // Mapeo explícito de la columna esquina4
    private double esquina4;

    private double accuracy;
}
