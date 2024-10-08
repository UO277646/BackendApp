package com.work.demo.repository;

import jakarta.persistence.*;
import lombok.*;

@Table(name="Fallos")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Fallo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fallo_id")
    private Long falloId; // CamelCase
    @ManyToOne
    @JoinColumn(name = "id_restriccion", nullable = false) // Mapeo explícito a proyecto_id
    private Restriccion restriccion;
    @Column(name = "datos") // No hace falta especificar, pero puede estar para claridad
    private String datos; // CamelCase
}
